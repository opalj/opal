#NonJavaBytecode
This category groups test cases that must be created manually and cannot be created from compiling
valid Java code. However, the resulting bytecode is still valid and does occur in real-world code.

All test cases within this category **cannot** be compiled using our pipeline.
 
##NJ2
[//]: # (MAIN: nj.Demo)
On Java's source level it is impossible to define two methods with matching names and signatures that
only vary in the method's return type. However, this can be done by crafting bytecode which is then
still valid.

>Please note: The project ```nonjava_bytecode2```, contained in the
```infrastructure_incompatible_testcases```, provides in ```scala/nj/EngineerBytecode``` a class that
is meant to engineer an instance of this case using OPAL's bytecode engineering DSL.  
```java
// nj/Demo.java
package nj;

import lib.annotations.callgraph.DirectCalls;
import lib.annotations.callgraph.DirectCall;

/**
 * @author Michael Reif
 */
public class Demo {

    @DirectCalls({
        @DirectCall(name="method", line=17, returnType = Object.class, resolvedTargets = "Lnj/Target;"),
        @DirectCall(name="method", line=18, returnType = String.class, resolvedTargets = "Lnj/Target;")
    })
    public static void main(String[] args) {
        Target t = new Target();
        t.method("42");
        t.method("42");
    }
}

class Target {

    public Object method(String s){
        System.out.println(s);
        System.out.println("Object");
        return s;
    }

    public String method(String s){
        return s;
    }
}

```
[//]: # (END)