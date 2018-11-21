#Java8DefaultInterfaceMethods
Tests the resolution of Java 8 default methods.

##J8DIM1
[//]: # (MAIN: j8dim.Class)
Tests the resolution of a polymorphic call when a class (cf.```j8dim.Class``` ) implements an
interface (cf. ```j8dim.Interface```) which declares a default method (cf. ```j8dim.Interface.method()```)and
inherits this default method from the inherited interface. A call on ```j8dim.Class.method()``` must
then be resolved to ```j8dim.Interface.method()```.

```java
// j8dim/Class.java
package j8dim;

import lib.annotations.callgraph.DirectCall;

class Class implements Interface {

    @DirectCall(name = "method", line = 10, resolvedTargets = "Lj8dim/Interface;")
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

##J8DIM2
[//]: # (MAIN: j8dim.SuperClass)
Tests the resolution of a polymorphic call when a class (cf. ```j8dim.SubClass```) implements an
interface with default method (cf. ```j8dim.Interface```) and extends a class (cf. ```j8dim.SuperClass```)
where the interface and the class define a method with the same signature, namely ```method```.
The subclass, inheriting from both, doesn't define a method with that signature, hence, the method
call on that class must be dispatched to the superclass's method when called on ```j8dim.Interface```.

```java
// j8dim/SuperClass.java
package j8dim;

import lib.annotations.callgraph.DirectCall;

class SuperClass {

    public void method(){
        // do something
    }

    @DirectCall(
            name = "method",
            line = 19,
            resolvedTargets = "Lj8dim/SuperClass;",
            prohibitedTargets = {"Lj8dim/Interface;"}
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

##J8DIM3
[//]: # (MAIN: j8dim.SuperClass)
Tests the resolution of a polymorphic call when a class (cf. ```j8dim.SubClass```) implements an
interface with default method (cf. ```j8dim.Interface```) and extends a class (cf. ```j8dim.SuperClass```)
where the interface and the class define a method with the same signature, namely ```method```.
The subclass, inheriting from both, doesn't define a method with that signature, hence, the method
call on that class must be dispatched to the superclass's method when called on ```j8dim.SuperClass```.


```java
// j8dim/SuperClass.java
package j8dim;

import lib.annotations.callgraph.DirectCall;

class SuperClass {

    public void method(){
        // do something
    }

    @DirectCall(
            name = "method",
            line = 19,
            resolvedTargets = "Lj8dim/SuperClass;",
            prohibitedTargets = {"Lj8dim/Interface;"}
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

##J8DIM4
[//]: # (MAIN: j8dim.SuperClass)
Tests the resolution of a polymorphic call when a class (cf. ```j8dim.SubClass```) implements an
interface with default method (cf. ```j8dim.Interface```) and extends a class (cf. ```j8dim.SuperClass```)
which doesn't provide a method with the same signature as the interface's default method, namely ```method```.
The subclass, inheriting from both, doesn't define a method with that signature, hence, the method
call on that class must be dispatched to the interface's method when called on ```j8dim.SubClass```.

```java
// j8dim/SuperClass.java
package j8dim;

import lib.annotations.callgraph.DirectCall;

class SuperClass {

    @DirectCall(
            name = "method",
            line = 14,
            resolvedTargets = "Lj8dim/Interface;"
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

##J8DIM5
[//]: # (MAIN: j8dim.SuperClass)
Tests the resolution of a polymorphic call when a class (cf. ```j8dim.Class```) extends an abstract
class (cf. ```j8dim.SuperClass```) that declares a method ```compute()``` and implements
several interfaces that are again in an inheritance relationship where all interfaces define a 
default method called ```method```. Respective calls on the ```compute``` and ```method``` methods
on ```j8dim.Class``` must then be dispatched to the correct methods. Since multiple interface define
the same method, the maximally specific methods must be computed (see JVM spec.). 

```java
// j8dim/SuperClass.java
package j8dim;

import lib.annotations.callgraph.DirectCalls;
import lib.annotations.callgraph.DirectCall;

abstract class SuperClass {

    public void compute(){ /* do something*/ }

    @DirectCalls({
        @DirectCall(
                name = "method",
                line = 26,
                resolvedTargets = "Lj8dim/DirectInterface;",
                prohibitedTargets = {"Lj8dim/Interface1;", "Lj8dim/Interface2;"}
        ),
        @DirectCall(
                name = "compute",
                line = 27,
                resolvedTargets = "Lj8dim/SuperClass;",
                prohibitedTargets = {"Lj8dim/Interface1;","Lj8dim/Interface2;"}
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

##J8DIM5
[//]: # (MAIN: j8dim.Demo)
An interface which extends two other interfaces that declare the same default method, as
```CombinedInterface``` does by extending ```SomeInterface``` and ```AnotherInterface```,
and also declares a default method (i.e. ```CombinedInterface.method```) it is possible to use
qualified super calls of the form ```SomeInterface.super.method()``` that must be resolved to the
respective super class' method implementation. The super calls must be qualified by the targeted
super interface since it is not unique otherwise. 
```java
// j8dim/Demo.java
package j8dim;

import lib.annotations.callgraph.DirectCalls;
import lib.annotations.callgraph.DirectCall;

class Demo {

    public static void main(String[] args){
        new CombinedInterface(){}.method();
    }
}

interface SomeInterface {
    default void method() {
        // do something
    }
}

interface AnotherInterface {
    default void method() {
        // do something
    }
}

interface CombinedInterface extends SomeInterface, AnotherInterface {
    
    @DirectCalls({
        @DirectCall(name = "method", line = 32, resolvedTargets = "Lj8dim/SomeInterface;"),
        @DirectCall(name = "method", line = 33, resolvedTargets = "Lj8dim/AnotherInterface;")
    })
    default void method() {
        SomeInterface.super.method();
        AnotherInterface.super.method();
    }
}
```
[//]: # (END)

#Java8StaticInterfaceMethods

Tests the correct method resolution of static interface methods.

Please note that the ```infrastructure_incompatible_testcases``` are more test cases w.r.t. to 
static interface methods pertaining to Java 9 and higher versions.
 
##J8SIM1
[//]: # (MAIN: j8sim.Class)
Tests the invocation of a static interface method ```j8sim.Interface``` in ```j8sim.Class```'s main
method.
```java
// j8sim/Class.java
package j8sim;

import lib.annotations.callgraph.DirectCall;

class Class {

    @DirectCall(name = "method", line = 9, resolvedTargets = "Lj8sim/Interface;")
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