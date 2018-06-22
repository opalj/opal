#Java8PolymorphicCalls
Tests the correct method resolution in the presence of Java 8
interfaces, i.e. default methods.
##J8PC1
[//]: # (MAIN: j8pc1/Class)
Tests the resolution of a polymorphic calls when a class implements an interface (with default method) and 
inherits the method from the inherited interface.

```java
// j8pc1/Class.java
package j8pc1;

import lib.annotations.callgraph.CallSite;

class Class implements Interface {
    
    @CallSite(name = "method", line = 10, resolvedTargets = "Lj8pc1/Interface;")
    public static void main(String[] args){ 
        Interface i = new Class();
        i.method();
    }
}

interface Interface { 
    default void method() {
        // do something
    }
}
```
[//]: # (END)

##J8PC2
[//]: # (MAIN: j8pc2/SuperClass)
Tests the resolution of a polymorphic calls when a class implements an interface (with default method) and extends a class
where the interface and the class define a method with the same signature. The subclass - inheriting from both - does not
define a method with that signature, hence, the method call on that class must be dispatched to the superclass's method **when
called on the interface**. 

```java
// j8pc2/SuperClass.java
package j8pc2;

import lib.annotations.callgraph.CallSite;

class SuperClass {
    
    public void method(){
        // do something
    }

    @CallSite(
            name = "method",
            line = 19,
            resolvedTargets = "Lj8pc2/SuperClass;",
            prohibitedTargets = {"Lj8pc2/Interface;"}
    )
    public static void main(String[] args){ 
        Interface i = new SubClass();
        i.method();
    }
}

interface Interface { 
    default void method() {
        // do something
    }
}

class SubClass extends SuperClass implements Interface {
    
}
```
[//]: # (END)

##J8PC3
[//]: # (MAIN: j8pc3/SuperClass)
Tests the resolution of a polymorphic calls when a class implements an interface (with default method) and extends a class
where the interface and the class define a method with the same signature. The subclass - inheriting from both - does not
define a method with that signature, hence, the method call on that class must be dispatched to the superclass's method **when
called on the class**. 

```java
// j8pc3/SuperClass.java
package j8pc3;

import lib.annotations.callgraph.CallSite;

class SuperClass {
    
    public void method(){
        // do something
    }

    @CallSite(
            name = "method",
            line = 19,
            resolvedTargets = "Lj8pc3/SuperClass;",
            prohibitedTargets = {"Lj8pc3/Interface;"}
    )
    public static void main(String[] args){ 
        SuperClass superClass = new SubClass();
        superClass.method();
    }
}

interface Interface { 
    default void method() {
        // do something
    }
}

class SubClass extends SuperClass implements Interface {
    
}
```
[//]: # (END)

##J8PC4
[//]: # (MAIN: j8pc4/SuperClass)
Tests the resolution of a polymorphic calls when a class implements an interface (with default method) and extends a class
where the method is only defined in the interface.

```java
// j8pc4/SuperClass.java
package j8pc4;

import lib.annotations.callgraph.CallSite;

class SuperClass {

    @CallSite(
            name = "method",
            line = 14,
            resolvedTargets = "Lj8pc4/Interface;"
    )
    public static void main(String[] args){ 
        SubClass subClass = new SubClass();
        subClass.method();
    }
}

interface Interface { 
    default void method() {
        // do something
    }
}

class SubClass extends SuperClass implements Interface {
    
}
```
[//]: # (END)

##J8PC5
[//]: # (MAIN: j8pc5/SuperClass)
Tests the resolution of a polymorphic calls when a class extends an abstract class that declares an abstract method.

```java
// j8pc5/SuperClass.java
package j8pc5;

import lib.annotations.callgraph.CallSites;
import lib.annotations.callgraph.CallSite;

class SuperClass {

    public void compute(){ /* do something*/ }

    @CallSites({
        @CallSite(
                name = "method",
                line = 26,
                resolvedTargets = "Lj8pc5/DirectInterface;",
                prohibitedTargets = {"Lj8pc5/Interface1;", "Lj8pc5/Interface2;"}
        ),
        @CallSite(
                name = "compute",
                line = 27,
                resolvedTargets = "Lj8pc5/SuperClass;",
                prohibitedTargets = {"Lj8pc5/Interface1;","Lj8pc5/Interface2;"}
        )
    })
    public static void main(String[] args){ 
        Class cls = new Class();
        cls.method();
        cls.compute();
    }
}

class Class extends SuperClass implements DirectInterface, Interface1, Interface2 {

}

interface Interface1 {
    
    void compute();
    
    default void method() {
        // do something
    }
}

interface Interface2 {
    
    void compute();
    
    default void method() {
            // do something
        }
}

interface DirectInterface extends Interface1, Interface2 { 
    default void method() {
        // do something
    }
}
```
[//]: # (END)

##J8PC6
[//]: # (MAIN: j8pc6/Class)
Tests the resolution of static interface methods.

```java
// j8pc6/Class.java
package j8pc6;

import lib.annotations.callgraph.CallSite;

class Class {
    
    @CallSite(name = "method", line = 9, resolvedTargets = "Lj8pc6/Interface;")
    public static void main(String[] args){ 
        Interface.method();
    }
}

interface Interface { 
    static void method() {
        // do something
    }
}
```
[//]: # (END)