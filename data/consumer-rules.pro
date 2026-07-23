# Consumer ProGuard rules for data module
-keep class com.pythonide.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
