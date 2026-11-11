# MotoCross - R8/ProGuard kuralları

# JNI köprüsü: C++ tarafı Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_* sembollerini
# sınıf ve metot adlarıyla çözer; bu nedenle isimler korunmalıdır.
-keep class com.oemcluster.pro.sensors.NativeTelemetryEngineBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.oemcluster.pro.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

