# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Keep sensor-related classes
-keep class com.sensortools.data.model.** { *; }
