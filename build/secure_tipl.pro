#
# This ProGuard configuration file illustrates how to process a program
# library, such that it remains usable as a library.
# Usage:
#     java -jar proguard.jar @library.pro
#

# Specify the input jars, output jars, and library jars.
# In this case, the input jar is the program library that we want to process.

-injars  TIPL_simple.jar
-outjars TIPL_basic.jar

-libraryjars <java.home>/lib/rt.jar

-libraryjars ../tipl-libraries/Jama.jar
-libraryjars ../tipl-libraries/mongo-db.jar
-libraryjars ../tipl-libraries/sezpoz-1.9.jar
-libraryjars ../tipl-libraries/lambdaj-with-dependencies.jar
-libraryjars ../tipl-libraries/ij.jar
-libraryjars ../tipl-libraries/com.springsource.javax.media.jai.codec.jar 
-libraryjars ../tipl-libraries/com.springsource.javax.media.jai.core.jar
-libraryjars ../tipl-libraries/jai_imageio.jar

-libraryjars  ../tipl-libraries/spark-core.jar

# Save the obfuscation mapping to a file, so we can de-obfuscate any stack
# traces later on. Keep a fixed source file attribute and all line number
# tables to get line numbers in the stack traces.
# You can comment this out if you're not interested in stack traces.

-printmapping TIPL_core.map
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,EnclosingMethod

# Preserve all annotations.

-keepattributes *Annotation*

# Preserve all public classes, and their public and protected fields and
# methods.

-keep public class * {
    public *;
}

# Preserve all .class method names.

#-keepclassmembernames class * {
#    java.lang.Class class$(java.lang.String);
#    java.lang.Class class$(java.lang.String, boolean);
#}

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve the special static methods that are required in all enumeration
# classes.

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your library doesn't use serialization.
# If your code contains serializable classes that have to be backward
# compatible, please refer to the manual.

#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}

# Your library may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface


# utilities
-keep public class tipl.util.*
# blocks
-keep public class * implements tipl.blocks.ITIPLBlock
# plugins
-keep public class * implements tipl.util.TIPLPluginManager.TIPLPluginFactory
# -keep public class * implements tipl.util.ITIPLPlugin

# io
-keep public class * implements tipl.formats.TReader
-keep public class * implements tipl.formats.TWriter
-keep public class * implements tipl.formats.TSliceWriter.DWFactory
-keep public class * implements tipl.formats.TReader.TSliceFactory

-dontwarn com.**
-dontnote com.**
-dontwarn net.**
-dontwarn javax.**
-dontnote javax.**
-dontwarn org.**
-dontnote org.**
-dontnote ij.**
-dontnote ch.**
-dontoptimize