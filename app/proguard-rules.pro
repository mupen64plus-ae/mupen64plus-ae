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
#-whyareyoukeeping

-keep, includedescriptorclasses class paulscode.android.mupen64plusae.jni.** { *; }

-dontwarn java.awt.event.*
-dontwarn java.awt.dnd.*
-dontwarn java.awt.*
-dontwarn javax.swing.*
-dontwarn java.awt.datatransfer.DataFlavor
-dontwarn java.awt.datatransfer.Transferable
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Area
-dontwarn java.awt.geom.PathIterator
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.DataBuffer
-dontwarn java.awt.image.DataBufferByte
-dontwarn java.awt.image.DataBufferInt
-dontwarn java.awt.image.DirectColorModel
-dontwarn java.awt.image.MultiPixelPackedSampleModel
-dontwarn java.awt.image.Raster
-dontwarn java.awt.image.SampleModel
-dontwarn java.awt.image.SinglePixelPackedSampleModel
-dontwarn java.awt.image.WritableRaster
-dontwarn javax.swing.text.JTextComponent
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
-dontwarn com.github.luben.zstd.BufferPool
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn com.github.luben.zstd.ZstdOutputStream
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.objectweb.asm.AnnotationVisitor
-dontwarn org.objectweb.asm.Attribute
-dontwarn org.objectweb.asm.ClassReader
-dontwarn org.objectweb.asm.ClassVisitor
-dontwarn org.objectweb.asm.FieldVisitor
-dontwarn org.objectweb.asm.Label
-dontwarn org.objectweb.asm.MethodVisitor
-dontwarn org.objectweb.asm.Type

-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-keep class org.apache.http.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class java.nio.file.** { *; }
-keep class java.nio.file.attribute.FileTime.** { *; }
-keep class org.eclipse.** { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }


#App specific code
-keep class paulscode.android.mupen64plusae.** { *; }

# Fix OAuth Drive API failure for release builds
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * { @com.google.api.client.util.Key <fields>; }
-keep class com.amazon.device.ads.** { *; }
-keep public class com.smaato.sdk.** { *; }
-keep public interface com.smaato.sdk.** { *; }
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.smaato.sdk.inject.Component
-dontwarn com.smaato.sdk.inject.Provides


# Google Mobile Ads SDK
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# Playwire SDK
-keep class com.intergi.playwiresdk.** { *; }
-keep class com.intergi.playwiresdk.ads.view.** { *; }
-keep class com.intergi.playwiresdk.headerbidding.** { *; }

-keepclassmembers class com.google.android.gms.ads.admanager.AdManagerAdRequest$Builder {
   public *;
}
