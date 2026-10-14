# Android manifest components and Jetpack Compose rules are kept by the Android
# Gradle plugin and library consumer rules. State uses explicit JSON field names,
# so no broad reflection or whole-package keep rules are necessary.
# Retain stack-trace line information for diagnosing release failures.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
