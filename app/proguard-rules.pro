# Ktor's JVM-only debugger detector references the java.management module.
# Android never executes that branch and does not ship these desktop classes.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# DTOs are serialized through generated kotlinx.serialization serializers.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
