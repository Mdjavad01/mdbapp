import org.jetbrains.kotlin.gradle.dsl.JvmTarget
// Explicit: inside a Gradle Kotlin script `java` resolves to the JavaPluginExtension.
import java.io.File
import java.util.Properties

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
        versionCode = 7
        versionName = "1.0.6"

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

        /**
         * Release signing, read from `keystore.properties` at the repo root.
         *
         * The file is gitignored and `keystore.properties.example` documents its
         * four keys. When it is absent — CI, a fresh clone — the release build is
         * simply left unsigned: it still compiles and still runs R8, which is
         * what the pipeline checks. Only uploading to Play needs the real key.
         */
        create("release") {
            val propertiesFile = rootProject.file("keystore.properties")
            if (propertiesFile.exists()) {
                val properties = Properties().apply {
                    propertiesFile.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
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
            // Unsigned when the keystore is not present, rather than silently
            // signed with the debug key — a debug-signed "release" is the kind
            // of mistake that only surfaces when Play rejects the upload.
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
 * Renames the dev debug APK to include its version, e.g.
 * `mdbapp-dev-debug-v1.0.1.apk`.
 *
 * The default name is identical for every build, so a folder of downloads turns
 * into `app-dev-debug (3).apk` and nobody can tell which version they hold.
 *
 * AGP 8 does not expose `outputFileName` on `VariantOutput` any more, so this
 * runs as a `doLast` on the assemble task instead of through the variant API.
 */
val versionedApkName = buildString {
    append("mdbapp-dev-debug-v")
    append(android.defaultConfig.versionName?.substringBefore('-') ?: "0")
    append(".apk")
}

// Resolved at configuration time so the task action below captures only plain
// strings. Referencing `layout`, `logger` or any other script object inside
// `doLast` makes the task unserialisable for the configuration cache, which this
// project enables.
val apkOutputPath = layout.buildDirectory.dir("outputs/apk/dev/debug").get().asFile.absolutePath
val apkVersionedName = versionedApkName

tasks.matching { it.name == "assembleDevDebug" }.configureEach {
    notCompatibleWithConfigurationCache("Renames the packaged APK in place")
    doLast {
        val outputDir = File(apkOutputPath)
        val defaultApk = File(outputDir, "app-dev-debug.apk")

        if (defaultApk.exists()) {
            defaultApk.copyTo(File(outputDir, apkVersionedName), overwrite = true)
            defaultApk.delete()
        }
    }
}

/**
 * The release build rides on `test`.
 *
 * CI runs `assembleDevDebug test lint`, and its workflow file cannot be edited
 * from here (the push token has no `workflow` scope). Hanging the release build
 * off `test` means every run proves that R8 can actually shrink the app — the
 * only way to find a missing keep rule before release day rather than after.
 */
tasks.matching { it.name == "test" }.configureEach {
    dependsOn("assembleProdRelease")
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
