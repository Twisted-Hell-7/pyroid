# ProGuard rules for data module
-dontwarn javax.annotation.**
-keep class kotlin.Metadata { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
