# Keep ZXing's format readers; they resolve by reflection.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room entities are constructed reflectively by the generated DAO layer.
-keep class com.quickscan.data.local.** { *; }

# Compose and Hilt ship their own consumer rules.
-dontwarn org.jetbrains.annotations.**