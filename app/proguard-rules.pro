-optimizationpasses 7
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively

# Remove TODOS os logs - ninguém vê nada no logcat
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Remove stack trace e nome dos arquivos fonte
-renamesourcefileattribute x
-keepattributes !SourceFile,!LineNumberTable,!LocalVariable*

# Manter só o necessário
-keep public class com.replayx.app.ui.LoginActivity { public *; }
-keep public class com.replayx.app.ui.MainActivity { public *; }
-keep public class com.replayx.app.ui.ParticleView { public *; }
-keep public class com.replayx.app.service.ReplayTransferService { public *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# AndroidX / Kotlin
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-dontwarn com.google.firebase.**
