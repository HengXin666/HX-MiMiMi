# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Compose rules (auto-applied via compose-compiler, kept here as fallback)
# -keep class androidx.compose.** { *; }

# Keep data classes used by R8 full mode
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Kotlin serialization (if used in the future)
# -keepattributes *Annotation*, InnerClasses
# -dontnote kotlinx.serialization.AnnotationsKt
# -keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
