# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Francisco\android-sdks/tools/proguard/proguard-android.txt
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

-dontobfuscate

-keep, includedescriptorclasses class paulscode.android.mupen64plusae.jni.** { *; }

-dontwarn java.awt.event.*
-dontwarn java.awt.dnd.*
-dontwarn java.awt.*
-dontwarn javax.swing.*
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue

-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-keep class org.apache.http.** { *; }
-keep class org.eclipse.** { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }