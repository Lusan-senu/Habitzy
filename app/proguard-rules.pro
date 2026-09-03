# Room entities
-keep class com.htj.habitzy.data.local.db.entity.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.htj.habitzy.** {
    *** Companion;
}
-keepclasseswithmembers class com.htj.habitzy.**$$serializer {
    *** INSTANCE;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
