-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.car.** { *; }
-keep class com.google.android.apps.auto.sdk.** { *; }
-keep class android.support.v4.app.** { *; }
-keep class com.aatorque.speedometer.** { *; }
