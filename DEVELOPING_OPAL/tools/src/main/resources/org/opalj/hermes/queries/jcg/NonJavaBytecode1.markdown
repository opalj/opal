#NonJavaBytecode
This category groups test cases that must be created manually and cannot be created from compiling
valid Java code. However, the resulting bytecode is still valid and does occur in real-world code.

All test cases within this category **cannot** be compiled using our pipeline. 
##NJ1
[//]: # (MAIN: tr.Demo)
In Java it is not possible to have a subclass that defines a method with the same name and
signature, but which is static. This is, however, possible at the bytecode level and the JVM
will call the default method.
 
This test case pertains to an evolution scenario and cannot be compiled out of the box. Please
proceed with the following steps to obtain all class files required by the test case:
 
Step one:
Compile the Main.java as is with the method with the markers *METHOD 1* (LINE 33) and *METHOD 3*
(LINE 58) uncommented and the method with the marker *METHOD 2* (LINE 37) commented. Then take the
resulting class files of "C", "Helper", "SuperIntf", "SubIntf", and "Main" and put tom into a
folder named "nj".
 
Step two:
Now comment the lines with the markers *METHOD 1* and *METHOD 3* and uncomment the line with the
marker *METHOD 2*. Then compile "Main.java" again and copy "Intf" class' class file to the folder
named "nj".
 
Step three:
Next to the folder "nj" add a folder named "META-INF" with the respective "MANIFEST.MF" file which
declares "nj/Main" as main class.
 *
Step four:
Compress both folders and create an executeable ".jar" file.
 */
```java
package nj;

import lib.annotations.callgraph.DirectCall;

interface SuperIntf {
/* METHOD 1 */ default void m(){ Helper.println("SuperIntf.m"); };
}

interface Intf extends SuperIntf {
/* METHOD 2 */  //  static void m(){ Helper.println("Intf.m"); };
}

interface SubIntf extends Intf {}

class C implements SubIntf {}

class Helper {
    public static void println(java.lang.String s) {
        System.out.println(s);
    }

}

public class Main {
    public static void main(String[] args) {
        run(new C());
    }

    @DirectCall(name = "m", line = 58, resolvedTargets = "Lnj/SuperIntf;", prohibitedTargets = "Lnj/Intf;")
    public static void run(SubIntf c) {
        // This invokes the default method from SuperIntf
/* METHOD 3 */ c.m();
    }

}
```
[//]: # (END)