# kotlinx.serialization: keep generated serializers for DTOs.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.sample.weekweather.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
# Retrofit service interfaces are accessed reflectively.
-keep,allowobfuscation interface com.sample.weekweather.data.remote.OpenMeteoApi
