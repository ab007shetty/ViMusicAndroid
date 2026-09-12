-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.abshetty.vimusic.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.abshetty.vimusic.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class io.ktor.client.engine.okhttp.** { *; }
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
