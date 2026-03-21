# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-keep class com.quizgame.data.model.** { *; }
-keepclassmembers class com.quizgame.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**
