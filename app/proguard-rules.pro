# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep classes for MySQL Connector/J
-keep class com.mysql.cj.** { *; }
-dontwarn com.mysql.cj.**

# Keep classes for Oracle Cloud Infrastructure (OCI) that are dynamically referenced
-keep class com.oracle.bmc.** { *; }
-dontwarn com.oracle.bmc.**

# Keep classes for Java Management Extensions (JMX)
-keep class java.lang.management.** { *; }
-dontwarn java.lang.management.**
-keep class javax.management.** { *; }
-dontwarn javax.management.**

# Keep classes for Java Naming and Directory Interface (JNDI)
-keep class javax.naming.** { *; }
-dontwarn javax.naming.**

# Keep classes for Java Authentication and Authorization Service (JAAS) and SASL
-keep class javax.security.auth.** { *; }
-dontwarn javax.security.auth.**
-keep class javax.security.sasl.** { *; }
-dontwarn javax.security.sasl.**

# Keep classes for XML processing
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**
-keep class javax.xml.transform.stax.** { *; }
-dontwarn javax.xml.transform.stax.**