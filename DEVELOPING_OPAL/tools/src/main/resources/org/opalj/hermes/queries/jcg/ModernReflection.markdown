#TrivialModernReflection
The strings are directly available. No control- or data-flow analysis is required.
##TMR1
[//]: # (MAIN: tmr1.Foo)
Tests modern reflection with respect to static methods.

```java
// tmr1/Foo.java
package tmr1;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Foo { 
    static String staticToString() { return "Foo"; }
    
    @IndirectCall(
        name = "staticToString", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr1/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Foo.class, "staticToString", methodType);
        String s = (String) handle.invokeExact();
    }
}
```
[//]: # (END)

##TMR2
[//]: # (MAIN: tmr2.Foo)
Tests modern reflection with respect to virtual calls.

```java
// tmr2/Foo.java
package tmr2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public String toString() { return "Foo"; }
    
    @IndirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr2/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findVirtual(Foo.class, "toString", methodType);
        String s = (String) handle.invokeExact(new Foo());
    }
}
```
[//]: # (END)

##TMR3
[//]: # (MAIN: tmr3.Foo)
Tests modern reflection with respect to constructor calls.

```java
// tmr3/Foo.java
package tmr3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Foo {

    @IndirectCall(name = "<init>", line = 14, resolvedTargets = "Ltmr3/Foo;")
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(void.class);
        MethodHandle handle = MethodHandles.lookup().findConstructor(Foo.class, methodType);
        Foo f = (Foo) handle.invokeExact();
    }
}
```
[//]: # (END)

##TMR4
[//]: # (MAIN: tmr4.Foo)
Uses modern reflection to retrieve a static field's value.

```java
// tmr4/Foo.java
package tmr4;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import lib.annotations.callgraph.CallSite;
class Foo {
    public String toString() { return "FOO"; }

    public static Foo f = new Foo();

    @CallSite(
        name = "toString", returnType = String.class, 
        line = 18, resolvedTargets = "Ltmr4/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findStaticGetter(Foo.class, "f", Foo.class);
        handle.invoke().toString();
    }
}

```

[//]: # (END)

##TMR5
[//]: # (MAIN: tmr5.Foo)
Uses modern reflection to retrieve an instance field's value.

```java
// tmr5/Foo.java
package tmr5;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import lib.annotations.callgraph.CallSite;
class Foo {
    public String toString() { return "Foo"; }

    public Foo f;

    public Foo() {
        this.f = this;
    }

    @CallSite(
        name = "toString", returnType = String.class, 
        line = 22, resolvedTargets = "Ltmr5/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findGetter(Foo.class, "f", Foo.class);
        handle.invoke(new Foo()).toString();
    }
}

```

[//]: # (END)

##TMR6
[//]: # (MAIN: tmr6.Bar)
Tests modern reflection with respect to special calls and invoke.

```java
// tmr6/Foo.java
package tmr6;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Bar extends Foo {
    protected String foo() { return "Bar"; }
    
    @IndirectCall(
        name = "foo", returnType = String.class, line = 18,
        resolvedTargets = "Ltmr6/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle handle = MethodHandles.lookup().findSpecial(Foo.class, "foo", methodType, Bar.class);
        String s = (String) handle.invokeExact(new Bar());
    }
}

class Foo {
    protected String foo() { return "Foo"; }
}
```
[//]: # (END)

##TMR7
[//]: # (MAIN: tmr7.Foo)
Tests modern reflection with respect to the invoke method that may
adapt its parameters and return type.

```java
// tmr7/Foo.java
package tmr7;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public static Object foo(String bar) { return bar; }

    @IndirectCall(
        name = "foo", returnType = Object.class, line = 18,
        resolvedTargets = "Ltmr7/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(Object.class, String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Foo.class, "foo", methodType);
        String s = (String) handle.invoke((Object)"Foo");
    }
}
```
[//]: # (END)

##TMR8
[//]: # (MAIN: tmr8.Foo)
Tests modern reflection with respect to the invokeWithArguments method.

```java
// tmr8/Foo.java
package tmr8;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public static String foo(String foo, String bar) { return foo + bar; }

    @IndirectCall(
        name = "foo", returnType = Object.class, line = 18,
        resolvedTargets = "Ltmr8/Foo;"
    )
    public static void main(String[] args) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class, String.class, String.class);
        MethodHandle handle = MethodHandles.lookup().findStatic(Foo.class, "foo", methodType);
        String s = (String) handle.invokeWithArguments(new Object[]{ "Foo", "Bar" });
    }
}
```
[//]: # (END)