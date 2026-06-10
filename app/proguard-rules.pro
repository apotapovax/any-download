# yt-dlp / FFmpeg (required for release builds)
-keep class com.yausername.** { *; }

# Commons Compress is used to unzip bundled Python; R8 breaks ExtraFieldUtils without these rules.
-keep class org.apache.commons.compress.** { *; }
-keep interface org.apache.commons.compress.** { *; }
-keep class org.apache.commons.** { *; }

-keep class commons-io.** { *; }
-keep class org.apache.commons.io.** { *; }

# Jackson is used when parsing yt-dlp metadata.
-keep class com.fasterxml.jackson.** { *; }

-keep class com.alexp.anydownload.BuildConfig { *; }

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions,SourceFile,LineNumberTable

-dontwarn org.apache.commons.**
-dontwarn org.bouncycastle.**
-dontwarn org.python.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.**
