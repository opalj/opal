#Java8Invokedynamics
Tests related to the invokedynamic instructions created by Java 8 method references and lambda expressions.
Please note, that the Scala compiler hijacks Java's infrastructure and analyses which support 
Java8's invokedynamics, i.e., those using `(Alt)LambdaMetaFactory`) will also support Scala to a
reasonable amount.

##MR1
[//]: # (MAIN: id.Class)
Tests method reference that deals with interface default methods (Java 8 or higher) which leads to
invokedynamics that use *INVOKEINTERFACE* methods handles.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class implements Interface {

    @FunctionalInterface public interface FIBoolean {
        boolean get();
    }

    @IndirectCall(
           name = "method", returnType = boolean.class, line = 18,
           resolvedTargets = "Lid/Interface;"
    )
    public static void main(String[] args){
        Class cls = new Class();
        FIBoolean bc = cls::method;
        bc.get();
    }
}

interface Interface {
    default boolean method() {
        return true;
    }
}
```
[//]: # (END)

##MR2
[//]: # (MAIN: id.Class)
Tests a method reference that results in an invokedynamic that uses an *INVOKESPECIAL* method handle which
is issued by calling a private method.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class {

    private String getTypeName() { return "Lid/Class;";}

    @IndirectCall(
       name = "getTypeName", returnType = String.class, line = 14,
       resolvedTargets = "Lid/Class;")
    public void callViaMethodReference(){
        java.util.function.Supplier<String> stringSupplier = this::getTypeName;
        stringSupplier.get();
    }

    public static void main(String[] args){
        Class cls = new Class();
        cls.callViaMethodReference();
    }
}
```
[//]: # (END)

##MR3
[//]: # (MAIN: id.Class)
Tests a method reference that results in an invokedynamic that uses an *INVOKESPECIAL* method handle which
is issued by calling a protected method from a super class that is resolved to a syntatic bridge
method compiled in ```id.Class```.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class extends SuperClass {

    @IndirectCall(
       name = "getTypeName", returnType = String.class, line = 12,
       resolvedTargets = "Lid/SuperClass;")
    public void callViaMethodReference(){
        java.util.function.Supplier<String> stringSupplier = super::getTypeName;
        stringSupplier.get();
    }

    public static void main(String[] args){
        Class cls = new Class();
        cls.callViaMethodReference();
    }
}

class SuperClass{
    protected String getTypeName() { return "Lid/SuperClass;";}
}
```
[//]: # (END)

##MR4
[//]: # (MAIN: id.Class)
Tests a method reference that results in an *INOVKESTATIC* method handle where the target method doesn't
have any formal arguments. The call is issued by calling a static method from a super class.

```java
// id/Class.java
package id;

import java.util.function.Supplier;
import lib.annotations.callgraph.IndirectCall;

class Class {

    @IndirectCall(
       name = "getTypeName", returnType = String.class, line = 13,
       resolvedTargets = "Lid/Class;")
    public static void main(String[] args){
        Supplier<String> stringSupplier = Class::getTypeName;
        stringSupplier.get();
    }

    static String getTypeName() { return "Lid/Class"; }
}
```
[//]: # (END)

##MR5
[//]: # (MAIN: id.Class)
Tests a method reference that results in an *INOVKESTATIC* method handle where the target method has
primitive formal arguments. The call is issued by a call on a functional interface.

```java
// id/Class.java
package id;

import java.util.function.Supplier;
import lib.annotations.callgraph.IndirectCall;

class Class {

    public static double sum(double a, double b) { return a + b; }

    @FunctionalInterface public interface FIDoubleDouble {
        double apply(double a, double b);
    }

    @IndirectCall(
       name = "sum", returnType = double.class, parameterTypes = {double.class, double.class}, line = 19,
       resolvedTargets = "Lid/Class;")
    public static void main(String[] args){
        FIDoubleDouble fidd = Class::sum;
        fidd.apply(1d,2d);
    }
}
```
[//]: # (END)

##MR6
[//]: # (MAIN: id.Class)
Tests a method reference that results in an invokedynamic that uses an *NEWINVOKESPECIAL* method handle
which is given by the method reference of ```id.Class::new```. Calling this method references
results in a constructor call to ```id.Class```.

```java
// id/Class.java
package id;

import java.util.function.Supplier;
import lib.annotations.callgraph.IndirectCall;

class Class {

    public Class(){}

    @IndirectCall(
       name = "<init>", line = 14, resolvedTargets = "Lid/Class;")
    public static void main(String[] args){
        Supplier<Class> classSupplier = Class::new;
        classSupplier.get();
    }
}
```
[//]: # (END)

##MR7
[//]: # (MAIN: id.Class)
Tests a method reference that results in an invokedynamic that uses an *INVOKEVIRTUAL* method handle
which is given by the method reference of ```cls::version``` where the actually called method is
implentend with ```id.Class```'s superclass ```id.SuperClass```.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class extends SuperClass{

    @IndirectCall(
       name = "version", returnType = String.class, line = 13,
       resolvedTargets = "Lid/SuperClass;")
    public static void main(String[] args){
        Class cls = new Class();
        java.util.function.Supplier<String> classSupplier = cls::version;
        classSupplier.get();
    }
}

class SuperClass {
    public String version() { return "1.0"; }
}
```
[//]: # (END)

#Lambdas
Test cases in the presence of lambdas.

##Lambda1
[//]: # (MAIN: id.Class)
Tests the invocation of a lamdba that results in an invokedynamic with an *INVOKESTATIC* method handle
which points to an synthetic method. Please not that all primitive integers are autoboxed to
```java.lang.Integer``` which then fits the lambdas (cf. ```isEven```) type.
```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;
import java.util.function.Function;

class Class {
    @IndirectCall(name = "doSomething", line = 13, resolvedTargets = "Lid/Class;")
    public static void main(String[] args){
        Function<Integer, Boolean> isEven = (Integer a) -> {
            doSomething();
            return a % 2 == 0;
        };
        isEven.apply(2);
    }

    private static void doSomething(){
        // call in lambda
    }
}
```
[//]: # (END)

##Lambda2
[//]: # (MAIN: id.Class)
Tests an invokedynamic invocation where the object receiver is captured in a lambda function.
Declaring a lambda function in another class (cf. ```id.LambdaProvider```) as it is invoked
(cf. ```id.Class```) leads to an *INVOKESTATIC* method handle where the receiver is not declared
within the same class.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class {

    public static void doSomething(){ }

    @IndirectCall(name = "doSomething", line = 12, resolvedTargets = "Lid/LambdaProvider;")
    public static void main(String[] args) {
        Runnable lambda = LambdaProvider.getRunnable();
        lambda.run();
    }
}

class LambdaProvider {

    public static void doSomething(){
        /* do something */
    }

    public static id.Runnable getRunnable(){
        return () -> LambdaProvider.doSomething();
    }
}
```
```java
// id/Runnable.java
package id;

@FunctionalInterface interface Runnable {
    void run();
}
```
[//]: # (END)

##Lambda3
[//]: # (MAIN: id.Class)
Tests the invocation of a lambda that was first written to and then retrieved from an array. This 
case results in an invokedynamic with an *INVOKESTATIC* method handle where the receiver argument
is read by *AASTORE* instruction form an array before the method invocation takes place.

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class {

     @FunctionalInterface interface Runnable {
        void run();
    }

    public static void doSomething(){
        /* do something */
    }

    public static Runnable[] lambdaArray = new Runnable[10];

    @IndirectCall(name = "doSomething", line = 22, resolvedTargets = "Lid/Class;")
    public static void main(String[] args) {
        Runnable r1 = () -> doSomething();
        lambdaArray[0] = r1;
        Runnable same = lambdaArray[0];
        same.run();
    }
}

final class Math {
    public static int PI(){
        return 3;
    }
}
```
[//]: # (END)

##Lambda4
[//]: # (MAIN: id.Class)
Tests the invocation of an intersection type lambda. This is special because the JVM does then use
the ```AltLambdaMetaFactory``` instead of the ```LambdaMetaFactory``` which is used for all the
previously defined cases. 

```java
// id/Class.java
package id;

import lib.annotations.callgraph.IndirectCall;

class Class {

    public interface MyMarkerInterface1 {}
    public interface MyMarkerInterface2 {}

    public @FunctionalInterface interface Runnable {
        void run();
    }

    public static void doSomething(){
        /* do something */
    }

    @IndirectCall(name = "doSomething", line = 21, resolvedTargets = "Lid/Class;")
    public static void main(String[] args) {
        Runnable run = (Runnable & MyMarkerInterface1 & MyMarkerInterface2) () -> doSomething();
        run.run();
    }
}
```
[//]: # (END)
