# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jeff/Development/android-sdk/tools/proguard/proguard-android.txt
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


# > Task :app:minifyFlavorAtestReleaseWithR8 FAILED
# ERROR: Missing classes detected while running R8. Please add the missing classes or apply additional keep rules that are generated in /home/runner/work/markor/markor/app/build/outputs/mapping/flavorAtestRelease/missing_rules.txt.
# ERROR: R8: Missing class java.awt.AlphaComposite (referenced from: java.awt.image.BufferedImage com.vladsch.flexmark.util.ImageUtils.makeRoundedCorner(java.awt.image.BufferedImage, int, int) and 1 other context)
-ignorewarnings
