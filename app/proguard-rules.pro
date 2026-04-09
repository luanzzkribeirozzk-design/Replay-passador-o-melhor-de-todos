# === OFUSCAÇÃO MÁXIMA ===
-optimizationpasses 7
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively

# Remover TODOS os logs
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

# Remover stack traces e source info
-renamesourcefileattribute x
-keepattributes !SourceFile,!LineNumberTable,!LocalVariable*,!SourceDir

# Manter só o mínimo necessário
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
