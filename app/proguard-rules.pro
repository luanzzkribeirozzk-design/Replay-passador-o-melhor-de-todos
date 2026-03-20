# ══════════════════════════════════════════════════════
# PROGUARD EXTREMO — PROTEÇÃO MÁXIMA
# ══════════════════════════════════════════════════════

# Passes de otimização máximos
-optimizationpasses 7
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Algoritmos de otimização agressivos
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,code/removal/advanced,code/allocation/variable

# Remover TODOS os logs sem exceção
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
    public static int println(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
-assumenosideeffects class java.lang.System {
    public static void exit(int);
}

# Remover System.out
-assumenosideeffects class java.lang.System {
    public static java.io.PrintStream out;
    public static java.io.PrintStream err;
}

# Manter só o mínimo necessário para funcionar
-keep public class com.replayx.app.ui.LoginActivity { public *; }
-keep public class com.replayx.app.ui.MainActivity { public *; }
-keep public class com.replayx.app.ui.ParticleView { public *; }
-keep public class com.replayx.app.service.ReplayTransferService { public *; }

# Classe de segurança — NÃO manter (ofuscar completamente)
# C.java, IntegrityCheck.java, TamperGuard.java serão totalmente renomeados

# Ofuscação de source files
-keepattributes !SourceFile,!LineNumberTable
-renamesourcefileattribute x

# Remover anotações desnecessárias
-keepattributes !Deprecated,!Signature,!EnclosingMethod,!InnerClasses

# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# AndroidX mínimo
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.**

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# Material
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Firebase REST
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Remover checagens de nulos do Kotlin (reduz tamanho)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
    static void throwNpe();
    static void throwJavaNpe();
    static void throwAssert();
    static void throwAssert(java.lang.String);
}

# Nomes de classe aleatorios para confundir análise
-repackageclasses ""
-allowaccessmodification
-mergeinterfacesaggressively
