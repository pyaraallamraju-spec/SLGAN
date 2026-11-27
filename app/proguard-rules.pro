# Conservative first-pass rules: keep app-owned classes intact to protect reflection and SDK callbacks.
# R8 can still remove unused dependency code and shrink unused packaged resources.
-keep class com.pyara.slgan.** { *; }

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
