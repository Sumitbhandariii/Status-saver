# Add project specific ProGuard rules here.
# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Models
-keep class com.example.data.model.** { *; }

