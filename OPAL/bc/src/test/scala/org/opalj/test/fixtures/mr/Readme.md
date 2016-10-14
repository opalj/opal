#Overview
This package contains code to engineer classes to test method resolution in
those cases that are not possible to create using Java source code.

The following is the generic stub used by the subsequent examples.
```
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


##StaticAndDefaultInterfaceMethods
The following is **pseudo-code** to facilitate comprehension of the test case:
***(The code is not valid Java code!)*** 

The engineered classes are found in `bc/test/resources/StaticAndDefaultInterfaceMethods/mr`.

```
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

##MaximallySpecificInterfaceMethods
The following is **pseudo-code** to facilitate comprehension of the test case:
***(The code is not valid Java code!)*** 

The engineered classes are found in `bc/test/resources/MaximallySpecificInterfaceMethods/mr`.

```
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
    // this is the maximally specific method w.r.t. Intf.m()
    default void m(){ Helper.println("S0_2.m"); };
}
/* Won't compile in Java due to conflicting methods: */ interface S2_1 extends S1_a, S1_c { }
interface S2_2 extends S0_2 { }
/* Won't compile in Java due to conflicting methods: */ interface Intf extends S2_1, S2_2 { }
```