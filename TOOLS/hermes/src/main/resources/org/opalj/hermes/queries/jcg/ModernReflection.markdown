#TrivialModernReflection
Reflective calls using the ```java.lang.invoke.*``` APIs and Java 7’s ```MethodHandle``` API which
are not signature polymorphic.

All strings and constants are directly available and thus no control- or data-flow analysis is required.
##TMR1
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findStatic``` method handle lookup where the
declaring class type, the static method's name, and the method's return type is given within the
the main method of ```tmr.Demo```. Afterwards, ```invokeExact``` is called on the looked up method
handle which results in a call to ```staticToString```.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Demo {
    static String target() { return "Demo"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Demo.class, "target", methodType);
        String s = (String) handle.invokeExact();
    }
}
```
[//]: # (END)

##TMR2
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findVirtual``` method handle lookup where the
declaring class type, the instance method's name, and the method's method type object is given within the
the main method of ```tmr.Demo```. Afterwards, ```invokeExact``` is called on the looked up method
handle which results in a call to ```tmr.Demo.target```.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Demo {
    public String target() { return "Demo"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findVirtual(Demo.class, "target", methodType);
        String s = (String) handle.invokeExact(new Demo());
    }
}
```
[//]: # (END)

##TMR3
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findConstructor``` method handle lookup where the
declaring class type and the method's method type object is given within the
the main method of ```tmr.Demo```. Afterwards, ```invokeExact``` is called on the looked up method
handle which results in a call to ```tmr.Demo```'s constructor.

Whether the constructor is called or not is verified by a static method call within the constructor.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.DirectCall;
class Demo {

    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(void.class);
        MethodHandle handle = MethodHandles.lookup().findConstructor(Demo.class, methodType);
        Demo f = (Demo) handle.invokeExact();
    }

    @DirectCall(name="verifyCall", line=18, resolvedTargets = "Ltmr/Demo;")
    public Demo() {
        Demo.verifyCall();
    }

    public static void verifyCall(){ /* do something */ }
}
```
[//]: # (END)

##TMR4
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findStaticGetter``` method handle lookup where the
declaring class type, the field's name, and the static field's type is given within the
the main method of ```tmr.Demo```. Afterwards, ```invoke``` is called on the looked up method
handle which results in a call to ```Demo.target``` on ```tmr.Demo.field```.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public String toString() { return "42"; }

    public static Demo field = new Demo();

    @DirectCall(
        name = "toString", returnType = String.class,
        line = 18, resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findStaticGetter(Demo.class, "field", Demo.class);
        handle.invoke().toString();
    }
}

```

[//]: # (END)

##TMR5
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findGetter``` method handle lookup where the
declaring class type, the field's name, and the field's type is given within the
the main method of ```tmr.Demo```. Afterwards, ```invoke``` is called with a new instance of
```tmr.Demo``` on the looked up method handle which results in a call to ```Demo.target``` on
```tmr.Demo.field```.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public String toString() { return "42"; }

    public Demo field;

    public Demo() {
        this.field = this;
    }

    @DirectCall(
        name = "toString", returnType = String.class,
        line = 22, resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findGetter(Demo.class, "field", Demo.class);
        handle.invoke(new Demo()).toString();
    }
}
```

[//]: # (END)

##TMR6
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findSpecial``` method handle lookup where the
declaring class type, the method's name and signature, and the special caller are passed to method.
Afterwards, ```invoke``` is called with a new instance of ```tmr.Demo``` on the looked
up method handle which results in a call to ```Superclass.target``` on an instance of```tmr.Demo```.

```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Demo extends Superclass {
    protected String target() { return "42!"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr/Superclass;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findSpecial(Superclass.class, "target", methodType, Demo.class);
        String s = (String) handle.invokeExact(new Demo());
    }
}

class Superclass {
    protected String target() { return "42"; }
}
```
[//]: # (END)

##TMR7
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findStatic``` method handle lookup where the
declaring class type, the static method's name, and the method's signature is given within the
the main method of ```tmr.Demo```. Afterwards, ```invoke``` is called on the looked up method
handle which results in a call to ```target```. Due to the invocation of ```MethodHandle.invoke```
it is possible that the return- or parameter types are adapted by the JVM, i.e. types might be
(un)boxed. 
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Demo {
    public static Object target(String param) { return param; }

    @IndirectCall(
        name = "target", returnType = Object.class, parameterTypes = String.class, line = 18,
        resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(Object.class, String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Demo.class, "target", methodType);
        String s = (String) handle.invoke((Object)"Demo");
    }
}
```
[//]: # (END)

##TMR8
[//]: # (MAIN: tmr.Demo)
Tests modern reflection by performing a ```findStatic``` method handle lookup where the
declaring class type, the static method's name, and the method's signature is given within the
the main method of ```tmr.Demo```. Afterwards, ```invokeWithArguments``` is called on the looked up method
handle which results in a call to ```target``` with where the respective parameters are passed.
```java
// tmr/Demo.java
package tmr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Demo {
    public static String target(String param1, String param2) { return param1 + param2; }

    @IndirectCall(
        name = "target", returnType = String.class, parameterTypes = { String.class, String.class },
        line = 18, resolvedTargets = "Ltmr/Demo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class, String.class, String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Demo.class, "target", methodType);
        String s = (String) handle.invokeWithArguments(new Object[]{ "42", "42" });
    }
}
```
[//]: # (END)
