# =============================================================================
# mdbapp — R8 / ProGuard rules
#
# The release build runs with `--obfuscate` (see README) so that the mapping
# file must be uploaded to Play Console for de-obfuscated crash reports.
# =============================================================================

# --- Kotlin metadata ---------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# --- kotlinx.serialization ---------------------------------------------------
# Serializers are generated at compile time; keep them and their companions.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
# Keep the @SerialName values readable in the mapping file for easier debugging.
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.SerialName <fields>;
}

# --- Retrofit ----------------------------------------------------------------
# Retrofit reads generic signatures and annotations reflectively.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# Keep our own API interfaces (they are annotated with @retrofit2.http.*).
-keep interface com.mrdastbaaf.mdbapp.**.data.remote.** { *; }

# --- OkHttp ------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Hilt / Dagger -----------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# --- Media3 / ExoPlayer ------------------------------------------------------
# Media3 instantiates renderers and extractors reflectively from class names.
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.decoder.** { *; }

# --- AndroidX -----------------------------------------------------------------
-keep class androidx.lifecycle.DefaultLifecycleObserver

# --- Domain models -----------------------------------------------------------
# Keep DTOs readable so the obfuscated stack traces stay diagnosable. This is a
# deliberate trade-off: the class *names* are not secret, the API contract is.
-keep class com.mrdastbaaf.mdbapp.**.data.remote.dto.** { *; }

# --- Strip logging in release ------------------------------------------------
# Belt and braces: the app never logs tokens, tickets or video URLs, and the
# logging interceptor is compiled out of release builds. This additionally
# removes the remaining android.util.Log calls.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
