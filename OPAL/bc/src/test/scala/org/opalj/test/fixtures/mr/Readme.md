# Overview
This package contains code to engineer classes to test method resolution in
those cases that are not possible to create using Java source code.

The following is the generic stub used by the subsequent examples.
```java
class C implements Intf {
    public void f(){ this.m(); }
}

class Helper {
    public static void println(java.lang.String s) {
        System.out.println(s);
    }
}

public class Main {
    public static void main(String[] args) {
        C c  = new C();
        c.f();
    }
}
```

## "Inherited"StaticMethods
The following is the Java code to facilitate comprehension of the test case.

The engineered classes are found in `bc/test/resources/InheritedStaticMethods/mr`.
***(The code is not valid Java bytecode; it is intended to test the resolution scheme used by the call graph algorithm!)***

```java
class X {
    static void m(){ Helper.println("X.m"); };
}

class SubX implements X { }

class Main {
    public static void main(String[] args) {
        // The static method m is "inherited"...
        SubX.m();

        // The static method m defined by the
        // interface I cannot be called using the
        // sub interface SubI (no lookup in supertype!)
        SubI.m();// => NoSuchMethodError ...
    }
}    
```

## Java 8 Method Call Semantics



### StaticAndDefaultInterfaceMethods
The following is **pseudo-code** to facilitate comprehension of the test case:
***(The code is not valid Java code!)***

The engineered classes are found in `bc/test/resources/StaticAndDefaultInterfaceMethods/mr`.

```java
interface SuperIntf {
    default void m(){ Helper.println("SuperIntf.m"); };
}

interface Intf extends SuperIntf {
    // In Java it is not possible to have a subclass that defines a
    // method with the same name and signature, but which is static.
    // This is, however, possible at the bytecode level and the JVM
    // will call the default method.
    static void m(){ Helper.println("Intf.m"); };
}
```

### MaximallySpecificInterfaceMethods
The following is **pseudo-code** to facilitate comprehension of the test case:
***(The code is not valid Java code!)***

The engineered classes are found in `bc/test/resources/MaximallySpecificInterfaceMethods/mr`.

```java
interface S0_1 {
    default void m(){ Helper.println("S0_1.m"); };
}
interface S0_2 {
    default void m(){ Helper.println("S0_2.m"); };
}
interface S1_a extends S0_1 {
    void m();
}
interface S1_c extends S0_1, S0_2 {
    // this is the maximally specific method w.r.t. m()
    default void m(){ Helper.println("S1_c.m"); };
}
/* Won't compile in Java due to conflicting methods: */ interface S2_1 extends S1_a, S1_c { }
interface S2_2 extends S0_2 { }
/* Won't compile in Java due to conflicting methods: */ interface Intf extends S2_1, S2_2 { }
```
