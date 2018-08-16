#BasicPolymorphicCalls
This tests aim to test basic virtual calls.

##BPC1
[//]: # (MAIN: bpc1/Class)
Tests a virtually dispatched method call which is in fact monomorphic.

```java
// bpc1/Class.java
package bpc1;

import lib.annotations.callgraph.CallSite;

class Class {
    
    public void method(){ }
    
    @CallSite(name = "method", line = 12, resolvedTargets = "Lbpc1/Class;")
    public static void main(String[] args){ 
        Class cls = new Class();
        cls.method();
    }
}
```
[//]: # (END)

##BPC2
[//]: # (MAIN: bpc/Class)
Tests a virtually dispatched method call when a simple type hierarchy is present.

```java
// bpc/Class.java
package bpc;

import lib.annotations.callgraph.CallSite;

class Class {
    
    public void method(){ }
    
    @CallSite(name = "method", line = 11, resolvedTargets = "Lbpc/SubClass;")
    public static void callMethod(Class cls) {
        cls.method();
    }
    
    public static void main(String[] args){
        callMethod(new SubClass());
    }
}

class SubClass extends Class {
    
    public void method() { }
}
```
[//]: # (END)

##BPC3
[//]: # (MAIN: bpc3/Class)
Tests a virtually dispatched method call when the receiver is an interface type.

```java
// bpc3/Class.java
package bpc3;

import lib.annotations.callgraph.CallSite;

interface Interface {
    void method();
}

class Class {
    
    public void method(){ }
 
    @CallSite(name = "method", line = 15, resolvedTargets = {"Lbpc3/ClassImpl;"}, prohibitedTargets ={"Lbpc3/Class"})
    public static void callOnInterface(Interface i){
        i.method();
    }
    
    public static void main(String[] args){
        callOnInterface(new ClassImpl());
    }
}

class ClassImpl implements Interface {
    public void method(){ }
}
```
[//]: # (END)

##BPC4
[//]: # (MAIN: bpc4/Class)
Tests a virtually dispatched method call when the receiver is loaded from an array.

```java
// bpc4/Class.java
package bpc4;

import lib.annotations.callgraph.CallSite;

interface Interface {
    void method();
}

class Class implements Interface {
    
    public static Interface[] types = new Interface[]{new Class(), new ClassImpl()};
    
    public void method(){ }
 
    @CallSite(name = "method", line = 18, resolvedTargets = "Lbpc4/Class;")
    public static void main(String[] args){
        Interface i = types[0];
        i.method();
    }
}

class ClassImpl implements Interface {
    public void method(){ }
}
```
[//]: # (END)