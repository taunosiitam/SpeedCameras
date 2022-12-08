-dontobfuscate

-keep class * extends android.app.Activity
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

#-printconfiguration
#-printusage
