#Non-virtual Calls
Tests related to non-virtual methods: static method calls, constructor calls (initializers),
super method calls, and private method calls.

Static initializers are found in their own category due to the inherent complexity of resolving them
in a meaningful manner.

##NVC1
[//]: # (MAIN: nvc.Class)
Tests the resolution of a static method call when another static method with the same name but
different signature is presence.
```java
// nvc/Class.java
package nvc;

import lib.annotations.callgraph.DirectCall;

class Class {

    public static void method(){ /* do something*/}
    public static void method(int param){ /* do something*/}

    @DirectCall(name = "method", line = 12, resolvedTargets = "Lnvc/Class;")
    public static void main(String[] args){
        Class.method();
    }
}
```
[//]: # (END)

##NVC2
[//]: # (MAIN: nvc.Class)
Tests the call resolution of default constructors which is caused by using Java's ```NEW``` keyword. The resulting 
bytecode contains a *INVOKESPECIAL* instruction which must be resolved to ```nvc.Class```'s ```<init>``` method, i.e.,
the default name for a constructor.
```java
// nvc/Class.java
package nvc;

import lib.annotations.callgraph.DirectCall;

public class Class {

    public Class(){

    }

    @DirectCall(name = "<init>", line = 13, resolvedTargets = "Lnvc/Class;")
    public static void main(String[] args){
        Class cls = new Class();
    }
}
```
[//]: # (END)

##NVC3
[//]: # (MAIN: nvc.Class)
Tests the resolution of a private method call when another method with the same name but
different signature is presence.
```java
// nvc/Class.java
package nvc;

import lib.annotations.callgraph.DirectCall;

class Class {

    private void method(){ /* do something*/}
    private void method(int num){ /* do something*/}

    @DirectCall(name = "method", line = 13, resolvedTargets = "Lnvc/Class;")
    public static void main(String[] args){
        Class cls = new Class();
        cls.method();
    }
}
```
[//]: # (END)

##NVC4
[//]: # (MAIN: nvc.Class)
Tests the resolution of a super call in the form `super.<methodName>`. The method call should
only be propagated to the immediate super class.
```java
// nvc/Class.java
package nvc;

import lib.annotations.callgraph.DirectCall;

class Class extends Superclass {

    @DirectCall(name = "method", line = 9, resolvedTargets = "Lnvc/Rootclass;")
    protected void method(){
        super.method();
    }

    public static void main(String[] args){
        Class cls = new Class();
        cls.method();
    }
}

class Superclass extends Rootclass {
    
}

class Rootclass {
    protected void method(){ /* do something relevant */ }
}
```
[//]: # (END)

##NVC5
[//]: # (MAIN: nvc.Demo)
Tests the resolution of a super call in a larger type hierarchy. In a class hierarchy like below,
with ```nvc.Sub <: nvc.Middle <: nvc.Super```, the super call in ```nvc.Sub.method``` will always invoke
```nvc.Middle.method``` even if ```Sub``` was compiled when ```nvc.Middle``` did not yet have an
implementation of ```method``` and thus the ```invokespecial``` references ```nvc.Super```.
```java
// nvc/Demo.java
package nvc;

import lib.annotations.callgraph.DirectCall;

public class Demo {
    
    public static void main(String[] args){
      new Sub().method();
    }
}

class Super { 
    
    void method() { /* doSomething */ } 
}

class Middle extends Super {
    
    void method() { /* doSomething */ }
}

class Sub extends Middle {
    
    @DirectCall(name="method", line=25, resolvedTargets = "Lnvc/Middle;")
    void method() { 
        super.method(); 
    }
}
```
[//]: # (END)