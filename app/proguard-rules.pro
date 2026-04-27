# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- General Kotlin & Coroutines ---
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# --- Kotlinx Serialization ---
-dontnote kotlinx.serialization.SerializationKt
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.* { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep classes marked with @Serializable
-keep @kotlinx.serialization.Serializable class * {
    # Keep the companion object (which contains the serializer)
    static ** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# --- Retrofit ---
# Retrofit uses reflection to inspect interface methods and their return types
# (including generic types like Call<T> or suspend functions).
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# --- App Specific ---
# Keep data models - absolutely essential for JSON parsing
-keep class com.keran.appoptmanager.model.** { *; }
-keepclassmembers class com.keran.appoptmanager.model.** { *; }

# Keep ApiService interface - absolutely essential for Retrofit
-keep class com.keran.appoptmanager.data.UpdateApiService { *; }
-keepclassmembers class com.keran.appoptmanager.data.UpdateApiService { *; }

# Prevent R8 from being too aggressive with interfaces
-keep interface com.keran.appoptmanager.data.UpdateApiService

# --- Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.material3.** { *; }

# --- Pinyin4j ---
-dontwarn net.sourceforge.pinyin4j.**
-dontwarn com.sun.**
-keep class net.sourceforge.pinyin4j.** { *; }
-keep class com.sun.** { *; }

# --- Coil ---
-dontwarn coil.**
-keep class coil.** { *; }
