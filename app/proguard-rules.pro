# ============================================================
# Phoenix (SecureChat) ProGuard/R8 Rules
# Aggressive shrinking and obfuscation for release builds
# ============================================================

# --- General ---
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Remove all logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keepclassmembers class * { @dagger.hilt.* *; }
-keepclassmembers class * { @javax.inject.* *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * { @androidx.room.* *; }

# --- Signal Protocol ---
-keep class org.signal.libsignal.** { *; }
-keep class org.whispersystems.** { *; }

# --- WebRTC ---
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# --- SQLCipher ---
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# --- Retrofit / OkHttp / Gson ---
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# Keep API response/request data classes
-keep class com.securechat.phoenix.auth.** { *; }
-keep class com.securechat.phoenix.crypto.models.** { *; }
-keep class com.securechat.phoenix.media.network.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Compose ---
-dontwarn androidx.compose.**

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# --- Enums (needed for Room + Gson) ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Parcelable ---
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
