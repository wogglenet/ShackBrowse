-keep class !android.support.v7.internal.view.menu.**,** {*;}


# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

-ignorewarnings

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep public class com.pierfrancescosoffritti.youtubeplayer.** {
   public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

-keepnames class com.pierfrancescosoffritti.youtubeplayer.*

-dontobfuscate

-dontwarn com.squareup.okhttp.**