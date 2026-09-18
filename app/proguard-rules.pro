# Keep Room entities
-keep class com.thrustwealth.app.data.model.** { *; }

# SaleSmartly Chat SDK
-keep class com.salesmartly.chatwidget.api.** { *; }
-keep class com.salesmartly.chatwidget.bridge.** { *; }
-keep class com.salesmartly.chatwidget.** { *; }

# Socket.IO / OkHttp / Retrofit (SaleSmartly transitive)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn io.socket.**
-keep class io.socket.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
