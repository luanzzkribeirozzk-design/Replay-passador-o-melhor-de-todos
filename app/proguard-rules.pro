# Criptografia Máxima de Segurança - Yguix
-optimizationpasses 9
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses 'com.replayx.app.obfuscated'
-flattenpackagehierarchy 'com.replayx.app.obfuscated'
-verbose

# Remover logs completamente
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Manter só o necessário para funcionar
-keep public class com.replayx.app.ui.LoginActivity { public *; }
-keep public class com.replayx.app.ui.MainActivity { public *; }
-keep public class com.replayx.app.ui.ParticleView { public *; }
-keep public class com.replayx.app.service.ReplayTransferService { public *; }

# Ofuscar TUDO mais (incluindo a classe C de segurança)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Firebase REST (não usa SDK, só HTTP)
-dontwarn com.google.firebase.**
