# SPYTube v1.3 ProGuard Rules

# ===== Retrofit =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ===== Gson =====
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ===== Coil =====
-keep class coil.** { *; }
-dontwarn coil.**

# ===== Models (needed for Gson deserialization) =====
-keep class com.spytube.app.models.** { *; }
-keepclassmembers class com.spytube.app.models.** { *; }

# ===== Kyant Backdrop (Liquid Glass) =====
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }
-dontwarn com.kyant.**

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ===== ExoPlayer / Media3 =====
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ===== Compose =====
-dontwarn androidx.compose.**

# ===== Parcelable (MediaItem) =====
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== Serializable =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== Glide =====
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ===== Keep enums =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
