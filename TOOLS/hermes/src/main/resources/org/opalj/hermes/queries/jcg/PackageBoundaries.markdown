#Package Boundaries

Visibility modifiers influence the call resolution as other language features (e.g. virtual dispatch).
Especially package visibility sometimes permits the resolution to some target method even if they are
public. The following test cases target mostly the method resolution of inter-package method calls.

##PB1
[//]: # (MAIN: a/Main)
Tests the resolution of a call site with a public receiver type (```a.A```) but a package visible method where the
actual receiver type is a class defined in another package (```b```). The receiver type's class
(```b.B```) also declares a package visible method with the exact same signature. However,
due to the access modifier ```package visible```, the call is resolved to the declared type of the
variable. It's prohibited to resolve the call to the package visible method in package ```b```. 
```java
// a/Main.java
package a;

import lib.annotations.callgraph.CallSite;

public class Main {
    
    @CallSite(name = "method", line = 10, resolvedTargets = "La/A;", prohibitedTargets = "Lb/B;")
    public static void main(String[] args){
        A a = new b.B();
        a.method();
    }   
}
```
```java
// a/A.java
package a;

public class A {
    
    void method(){
        /* do something */
    }
}
```
```java
// b/B.java
package b;

public class B extends a.A {
    
    void method(){
        /* do something */
    }
}
```
[//]: # (END)

##PB2
[//]: # (MAIN: a/A)
Tests the resolution of a call site with a public receiver type (```a.A```) but a package visible method where the
actual receiver type is a class defined in same package (```a```). The receiver type's class
(```a.C```) does not declare a method with the same name and inherits from a public type from another
package that declares it. However, due to the access modifier ```package visible```, the call is resolved
to the declared type of the variable and not to the supertype's implementation. 
It's prohibited to resolve the call to the package visible method in package ```b```. 
```java
// a/A.java
package a;

import lib.annotations.callgraph.CallSite;

public class A {
    
    @CallSite(name = "method", line = 10, resolvedTargets = "La/A;", prohibitedTargets = "Lb/B;")
    public static void main(String[] args){
        A a = new C();
        a.method();
    }
    
    void method(){
        /* do something */
    }
}
```
```java
// a/C.java
package a;

public class C extends b.B {
    
}
```
```java
// b/B.java
package b;

public class B extends a.A {
    
    void method(){
        /* do something */
    }
}
```
[//]: # (END)
