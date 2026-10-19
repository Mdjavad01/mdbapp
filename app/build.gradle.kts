import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Environment configuration.
 *
 * NOTE: `staging` currently points at a placeholder host — the staging subdomain
 * has not been provisioned yet. Until it is, use `devDebug`, which targets the
 * production API with verbose logging enabled and is restricted to read-only
 * endpoints plus the designated test account.
 */
android {
    namespace = "com.mrdastbaaf.mdbapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mrdastbaaf.mdbapp"
        minSdk = 24
        targetSdk = 35
        // Bumped on every shipped build: Android refuses to install an APK whose
        // versionCode is lower than the installed one, and a same-code reinstall
        // over a build signed with a different key is what produced the
        // "package conflicts with an existing package" error.
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Only fa is shipped; the default resources are Persian so the app is
        // correct regardless of the device locale.
        resourceConfigurations += setOf("fa")

        // Certificate pinning is opt-in at build time. The pin set lives in
        // network_security_config.xml and is only applied when this is true.
        buildConfigField("boolean", "ENABLE_CERT_PINNING", "false")
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "مستر دستباف (توسعه)")
            buildConfigField("String", "API_BASE_URL", "\"https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_STRICT_SECURITY", "false")
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            resValue("string", "app_name", "مستر دستباف (استیجینگ)")
            // TODO: replace with the real staging subdomain once provisioned.
            buildConfigField("String", "API_BASE_URL", "\"https://staging.mrdastbaaf.com/wp-json/mrdastbaaf/v1/\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_STRICT_SECURITY", "true")
        }

        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "مستر دستباف")
            buildConfigField("String", "API_BASE_URL", "\"https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_STRICT_SECURITY", "true")
        }
    }

    /**
     * A committed debug key.
     *
     * The default debug keystore is generated per machine, and GitHub-hosted
     * runners start empty — so every CI build was signed with a different key
     * and could not be installed over the previous one. Pinning it here makes
     * every build updatable in place.
     *
     * This is not a secret: the credentials are the well-known Android debug
     * ones and the key only ever signs debug builds.
     */
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        // Do not fail the build on translations we intentionally do not ship.
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

/**
 * Names the APK after the build, e.g. `mdbapp-dev-debug-v1.0.1.apk`.
 *
 * The default name (`app-dev-debug.apk`) is identical for every build, so a
 * folder of downloads turns into `app-dev-debug (3).apk` and nobody can tell
 * which version they are holding.
 */
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val version = output.versionName.orNull?.substringBefore('-') ?: "0"
            output.outputFileName.set(
                "mdbapp-${variant.flavorName}-${variant.buildType}-v$version.apk",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The project targets a production app: keep warnings visible.
        allWarningsAsErrors.set(false)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
