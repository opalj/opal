#TrivialReflection
The strings are directly available. No control- or data-flow analysis is required.
##TR1
[//]: # (MAIN: tr1.Foo)
Test static initializer call for class constant.

```java
// tr1/Foo.java
package tr1;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static int foo = 1;

    @IndirectCall(
        name = "<clinit>", line = 11, resolvedTargets = "Ltr1/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Class c = Foo.class;
    }
}
```
[//]: # (END)

##TR2
[//]: # (MAIN: tr2.Foo)
Test reflection with respect to static methods.

```java
// tr2/Foo.java
package tr2;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static String staticToString() { return "Foo"; }

    @IndirectCall(
        name = "staticToString", returnType = String.class, line = 12,
        resolvedTargets = "Ltr2/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo.class.getDeclaredMethod("staticToString").invoke(null);
    }
}
```
[//]: # (END)

##TR3
[//]: # (MAIN: tr3.Foo)
Test reflection with respect to instance methods.

```java
// tr3/Foo.java
package tr3;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public String toString() { return "Foo"; }

    @IndirectCall(
        name = "toString", returnType = String.class, line = 12,
        resolvedTargets = "Ltr3/Foo;"
    )
    void m() throws Exception {
        Foo.class.getDeclaredMethod("toString").invoke(this);
    }

    public static void main(String[] args) throws Exception { new Foo().m(); }
}
```
[//]: # (END)

##TR4
[//]: # (MAIN: tr4.Foo)
Test reflection with respect to instance methods retrieved via getMethod.

```java
// tr4/Foo.java
package tr4;

import lib.annotations.callgraph.IndirectCall;
public class Foo {
    public String toString() { return "Foo"; }

    @IndirectCall(
        name = "toString", returnType = String.class, line = 12,
        resolvedTargets = "Ltr4/Foo;"
    )
    void m() throws Exception {
        Foo.class.getMethod("toString").invoke(this);
    }

    public static void main(String[] args) throws Exception { new Foo().m(); }
}
```
[//]: # (END)

##TR5
[//]: # (MAIN: tr5.Foo)
Test reflection with respect to methods having parameters.

```java
// tr5/Foo.java
package tr5;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public static String m(String parameter) { return "Foo" + parameter; }

    @IndirectCall(
        name = "m", returnType = String.class, parameterTypes = String.class, line = 12,
        resolvedTargets = "Ltr5/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo.class.getDeclaredMethod("m", String.class).invoke(null, "Bar");
    }
}
```
[//]: # (END)

##TR6
[//]: # (MAIN: tr6.Foo)
Test reflection with respect to constructors.

```java
// tr6/Foo.java
package tr6;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public Foo(String s) {    }

    @IndirectCall(
        name = "<init>", parameterTypes = String.class, line = 12,
        resolvedTargets = "Ltr6/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo.class.getConstructor(String.class).newInstance("ASD");
    }
}
```
[//]: # (END)


##TR7
[//]: # (MAIN: tr7.Foo)
Test reflection with respect to the default constructor.

```java
// tr7/Foo.java
package tr7;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    public Foo() {    }

    @IndirectCall(name = "<init>", line = 9, resolvedTargets = "Ltr7/Foo;")
    public static void main(String[] args) throws Exception {
        Foo.class.newInstance();
    }
}
```
[//]: # (END)

##TR8
[//]: # (MAIN: tr8.Foo)
Test reflection used to retrieve a field.

```java
// tr8/Foo.java
package tr8;

import java.lang.reflect.Field;
import lib.annotations.callgraph.IndirectCall;
public class Foo {
    private Object field;

    @IndirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Ltr8/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo foo = new Foo();
        foo.field = new Foo();

        Field field = Foo.class.getDeclaredField("field");
        Object o = field.get(foo);
        o.toString();
    }

    public String toString() {
        return "Foo";
    }
}
```
[//]: # (END)

##TR9
[//]: # (MAIN: tr9.Foo)
Test reflection used to retrieve a field via getField.

```java
// tr9/Foo.java
package tr9;

import java.lang.reflect.Field;
import lib.annotations.callgraph.IndirectCall;
public class Foo {
    public Object field;

    @IndirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Ltr9/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo foo = new Foo();
        foo.field = new Foo();

        Field field = Foo.class.getField("field");
        Object o = field.get(foo);
        o.toString();
    }

    public String toString() {
        return "Foo";
    }
}
```
[//]: # (END)

##TR10
[//]: # (MAIN: tr10.Foo)
Test reflection with respect to forName.

```java
// tr10/Foo.java
package tr10;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static int foo = 1;

    @IndirectCall(
        name = "<clinit>", line = 11, resolvedTargets = "Ltr10/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Class.forName("tr10.Foo");
    }
}
```
[//]: # (END)


#LocallyResolvableReflection
The complete information is locally (intra-procedurally) available.
##LRR1
[//]: # (MAIN: lrr1.Foo)
Test reflection where the target class is dynamically decided.

```java
// lrr1/Foo.java
package lrr1;

import lib.annotations.callgraph.IndirectCall;
class Bar {
    static int bar = 2;
}
class Foo {
    static int foo = 1;

    public static void main(String[] args) throws Exception {
        m(args.length % 2 == 0);
    }

    @IndirectCall(
        name = "<clinit>", returnType = String.class, line = 19,
        resolvedTargets = { "Llrr1/Foo;", "Llrr2/Foo;" }
    )
    static void m(boolean b) throws Exception {
        Class.forName(b ? "lrr1.Foo" : "lrr1.Bar");
    }
}
```
[//]: # (END)

##LRR2
[//]: # (MAIN: lrr2.Foo1)
Tests reflection where the target class is dynamically decided and the result of a StringBuilder.

```java
// lrr2/Foo1.java
package lrr2;

import lib.annotations.callgraph.IndirectCall;
class Foo1 {
    static int foo = 1;

    @IndirectCall(
        name = "<clinit>", line = 17, resolvedTargets = { "Llrr2/Foo1;", "Llrr2/Foo2;" }
    )
    static void m(boolean b) throws Exception {
        String className = "lrr2.Foo";
        if (b)
            className += 1;
        else
            className += 2;
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
        m(args.length % 2 == 0);
    }
}
class Foo2 {
    static int foo = 2;
}
```
[//]: # (END)

##LRR3
[//]: # (MAIN: lrr3.Foo)
Test reflection where the target class is dynamically decided from a locally set instance field.

```java
// lrr3/Foo.java
package lrr3;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static int foo = 1;
    private String className;

    @IndirectCall(
        name = "<clinit>", line = 15, resolvedTargets = "Llrr3/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo foo = new Foo();
        foo.className = "lrr3.Foo";
        Class.forName(foo.className);
    }
}
```
[//]: # (END)

#ContextSensitiveReflection
The concrete strings require information about the context.

##CSR1
[//]: # (MAIN: csr1.Foo)
The class name is passed as an argument.

```java
// csr1/Foo.java
package csr1;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static int foo = 1;

    @IndirectCall(
        name = "<clinit>", line = 11, resolvedTargets = "Lcsr1/Foo;"
    )
    static void m(String className) throws Exception {
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
        m("csr1.Foo");
    }
}

class Bar {
    static String staticToString() { return "Bar"; }
}
```
[//]: # (END)

##CSR2
[//]: # (MAIN: csr2.Foo)
The class name is unknown.

```java
// csr2/Foo.java
package csr2;

import lib.annotations.callgraph.IndirectCall;
public class Foo {
    static int foo = 1;

    @IndirectCall(
        name = "<clinit>", line = 11, resolvedTargets = { "Lcsr2/Foo;", "Lcsr2/Bar;" }
    )
    static void m(String className) throws Exception {
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
        m(args[0]);
    }
}

class Bar {
    static int bar = 2;
}
```
[//]: # (END)

##CSR3
[//]: # (MAIN: csr3.Foo)
Test reflection with respect to a private instance field that is set in the initializer.

```java
// csr3/Foo.java
package csr3;

import lib.annotations.callgraph.IndirectCall;
class Foo {
    static int foo = 1;
    public static String className;

    @IndirectCall(
        name = "<clinit>", line = 13, resolvedTargets = "Lcsr3/Foo;"
    )
    public static void main(String[] args) throws Exception {
        Foo.className = args[0];
        Class.forName(Foo.className);
    }
}
```
[//]: # (END)
