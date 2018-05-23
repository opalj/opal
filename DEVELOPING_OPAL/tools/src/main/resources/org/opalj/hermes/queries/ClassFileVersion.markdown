# Class File Version
Extracts the class file version of each class file belonging to the project.

## Note
Class files using Java 1.0 and 1.1. are grouped together because they use the same class file version number.

## Major Features of Selected Class File Versions

### Java 6 and newer
Generally, don't have *jsr*/*ret* instructions anymore, because Java stopped generating them. However, technically they are still allowed and were only forbidden with version 51 (*cf. Java Virtual Machine Specification - Constraints on Java Virtual Machine code - Static Constraints*)

### Java 7 and newer
May have *invokedynamic* instructions; however, Java only started using *invokedynamic* with Java 8 and all other languages (e.g., Clojure, Groovy, Scala) also did not starte using them right away.

### Java 8 and newer
 - usually have *invokedynamic* instructions because Java started using it
 - added default methods (this changed the way how method resolution works)


## Attributes per Class File Version

<table>
<tr><th>Attribute</th><th>ClassFileVersion</th><th>Java SE</th></tr>
<tr><td>ConstantValue</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>Code</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>Exceptions</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>SourceFile</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>LineNumberTable</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>LocalVariableTable</td><td>45.3</td><td>1.0.2</td></tr>
<tr><td>InnerClasses</td><td>45.3</td><td>1.1</td></tr>
<tr><td>Synthetic</td><td>45.3</td><td>1.1</td></tr>
<tr><td>Deprecated</td><td>45.3</td><td>1.1</td></tr>
<tr><td>EnclosingMethod</td><td>49.0</td><td>5.0</td></tr>
<tr><td>Signature</td><td>49.0</td><td>5.0</td></tr>
<tr><td>SourceDebugExtension</td><td>49.0</td><td>5.0</td></tr>
<tr><td>LocalVariableTypeTable</td><td> 49.0</td><td>5.0</td></tr>
<tr><td>RuntimeVisibleAnnotations</td><td> 49.0</td><td> 5.0</td></tr>
<tr><td>RuntimeInvisibleAnnotations</td><td> 49.0</td><td> 5.0</td></tr>
<tr><td>RuntimeVisibleParameterAnnotations</td><td> 49.0</td><td> 5.0</td></tr>
<tr><td>RuntimeInvisibleParameterAnnotations</td><td> 49.0</td><td> 5.0</td></tr>
<tr><td>AnnotationDefault</td><td> 49.0</td><td> 5.0</td></tr>
<tr><td>StackMapTable</td><td> 50.0</td><td> 6</td></tr>
<tr><td>BootstrapMethods</td><td> 51.0</td><td> 7</td></tr>
<tr><td>RuntimeVisibleTypeAnnotations</td><td> 52.0</td><td> 8</td></tr>
<tr><td>RuntimeInvisibleTypeAnnotations </td><td>52.0</td><td> 8 </td></tr>
<tr><td>MethodParameters</td><td> 52.0</td><td> 8 </td></tr>
</table>
