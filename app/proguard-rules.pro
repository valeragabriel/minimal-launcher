-keepclassmembers class com.gabriel.minimal.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.gabriel.minimal.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
