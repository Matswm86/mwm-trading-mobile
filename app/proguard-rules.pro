# kotlinx.serialization — keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class no.mwmai.backtest.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class no.mwmai.backtest.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
