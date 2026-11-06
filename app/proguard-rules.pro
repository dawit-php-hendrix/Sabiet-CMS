# ProGuard & R8 Optimization Rules for Sabiet CMS

# Keep WebView and JavaScript interfaces
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Android Autofill
-keep class android.view.autofill.** { *; }

# Optimize and remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
