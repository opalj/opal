# Direct Calls
Tests that relate to this category are typically directly resolvable.
Hence, it covers static method calls, private calls, constructor calls, super calls, as well as
calls on the implicit this parameter. Every call of this category is inherently monomorphic.
##DC1
[//]: # (MAIN: dc1/Class)
Tests the resolution of a static method call when another static method with the same name but
different signature is presence.
```java
// dc1/Class.java
package dc1;

import lib.annotations.callgraph.CallSite;

class Class {
    
    public static void method(){ /* do something*/}
    public static void method(int param){ /* do something*/}
    
    @CallSite(name = "method", parameterTypes = int.class, line = 12, resolvedTargets = "Ldc1/Class;")
    public static void main(String[] args){
        Class.method();
    }
}
```
[//]: # (END)

##DC2
[//]: # (MAIN: dc/Class)
Tests the resolution of a constructor call (<init>)
```java
// dc/Class.java
package dc;

import lib.annotations.callgraph.IndirectCall;

public class Class {
    
    public Class(){
        
    }
    
    @IndirectCall(name = "<init>", line = 13, resolvedTargets = "Ldc/Class;")
    public static void main(String[] args){
        Class cls = new Class();
    }
}
```
[//]: # (END)

##DC3
[//]: # (MAIN: dc/Class)
Tests the resolution of a private method call when another method with the same name but
different signature is presence.
```java
// dc/Class.java
package dc;

import lib.annotations.callgraph.CallSite;

class Class {
    
    private void method(){ /* do something*/}
    private void method(int num){ /* do something*/}
    
    @CallSite(name = "method", line = 13, resolvedTargets = "Ldc/Class;")
    public static void main(String[] args){
        Class cls = new Class();
        cls.method();
    }
}
```
[//]: # (END)

##DC4
[//]: # (MAIN: dc/Class)
Tests the resolution of a super call in the form ```super.<methodName>```. The method call should
only be propagated to the immediate super class.
```java
// dc/Class.java
package dc;

import lib.annotations.callgraph.CallSite;

class Class extends Superclass {
    
    @CallSite(name = "method", line = 9, resolvedTargets = "Ldc/Superclass;", prohibitedTargets = "Ldc/Rootclass;")
    protected void method(){ 
        super.method(); 
    }
    
    public static void main(String[] args){
        Class cls = new Class();
        cls.method();
    }
}

class Superclass extends Rootclass {
    protected void method(){ /* do something relevant */ }
}

class Rootclass {
    protected void method(){ /* do something irrelevant */ }
}
```
[//]: # (END)