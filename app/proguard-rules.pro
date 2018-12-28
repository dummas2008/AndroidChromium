# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /work/android/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-ignorewarnings
# Fragments loaded by name via Fragment.instantiate(Context,String)
# Not all fragments in this package are PreferenceFragments. E.g. HomepageEditor
# is a normal Fragment.
-keep public class org.chromium.chrome.browser.preferences.** extends android.app.Fragment

# These classes aren't themselves referenced, but __ProcessService[0,1,2...] are
# referenced, and we look up these services by appending a number onto the name
# of the base class. Thus, we need to keep the base class name around so that
# the child classes can be looked up.
-keep class org.chromium.content.app.SandboxedProcessService
-keep class org.chromium.content.app.PrivilegedProcessService

-keep public class org.chromium.chrome.browser.BraveSyncWorker$JsObject
-keep public class * implements org.chromium.chrome.browser.BraveSyncWorker$JsObject
-keepclassmembers class org.chromium.chrome.browser.BraveSyncWorker$JsObject {
    <methods>;
}
-keep public class org.chromium.chrome.browser.BraveSyncWorker$JsObjectWordsToBytes
-keep public class * implements org.chromium.chrome.browser.BraveSyncWorker$JsObjectWordsToBytes
-keepclassmembers class org.chromium.chrome.browser.BraveSyncWorker$JsObjectWordsToBytes {
    <methods>;
}
-keepattributes JavascriptInterface

# SearchView is used in website_preferences_menu.xml and is constructed by
# Android using reflection.
-keep class android.support.v7.widget.SearchView {
  public <init>(...);
}

# This class member is referenced in BottomSheetBottomNav as a temporary
# measure until the support library contains a solution for disabling shifting
# mode. TODO(twellington): remove once support library has a fix and is rolled.
-keepclassmembers class android.support.design.internal.BottomNavigationMenuView {
    boolean mShiftingMode;
}

-dontwarn com.mixpanel.android.R**
