# Library
Tests the call-graph handling for the analysis of software libraries, i.e. partial programs. The main
difference between applications and library software is, that libraries are intended to be used, and,
therefore, to be extended. Library extensions can cause call-graph edges within the library that can
already be detected without a concrete application scenario. For instance, when the library contains yet
independent as well as by client code accessible and inheritable classes and interfaces that declare
a method with exactly the same method signature.
 
When the previous conditions are meet, a client can extend such a class as well as implement the
interface respectively without overriding the respective method which leads to interface call sites
that must be additionally resolved to the class' method. We refer to those edges as call-by-signature
(CBS) edges. 

According to [1], all test cases in that category assume that all packages are closed.
 
[1] Reif et al., Call Graph Construction for Java Libraries, FSE 2016.

##LIB1
[//]: # (LIBRARY)
Tests virtual call resolution in the context of libraries where the calling context is unknown. 
The circumstances of the virtual call are as follows:
1) We have a method ```public void libraryEntryPoint(Type type)``` which calls a method on the passed
parameter,
2) A type ```public class Type``` which declares a method ```public void method()```,
3) Another type ```public class Subtype extends Type``` which also declares a method ```public void method()```,
4) An additional type ```public class SomeType``` which also delcares a method ```public void method()```.
Since the calling context of ```Type.method()``` in ```Demo.entrypoint(Type t)``` is unknown. The call-graph
construction must assume that the parameter ```type``` can hold all possible subtypes of ```Type``` .
```java
// lib1/Demo.java
package lib1;

import lib.annotations.callgraph.CallSite;

public class Demo {
    
    @CallSite(name = "method", line = 10, resolvedTargets = {"Llib1/Type;", "Llib1/Subtype;"}, 
    prohibitedTargets = "Llib1/SomeType;")
    public void libraryEntryPoint(Type type){
        type.method();
    }
}
```
```java
// lib1/Type.java
package lib1;

public class Type {
    
    public void method(){
        /* do something */
    }
}
```
```java
// lib1/Subtype.java
package lib1;

public class Subtype extends Type {
    
    public void method(){
        /* do something */
    }
}
```
```java
// lib1/SomeType.java
package lib1;

public class SomeType {
    
    public void method(){
        /* do something */
    }
}
```
[//]: # (END)

##LIB2
[//]: # (LIBRARY)
Tests virtual call resolution in the context of libraries where the calling context is unknown. 
The circumstances of the virtual call are as follows:
1) We have a method ```public void libraryEntryPoint(Type type)``` which calls a method on the passed
parameter,
2) A type ```public class Type``` which declares a method ```public void method()```,
3) Another type ```public class Subtype extends Type``` which also declares a method ```public void method()```,
4) An additional type ```public class SomeType``` which also delcares a method ```public void method()```.
Since the calling context of ```Type.method()``` in ```Demo.callOnField()``` is unknown, i.e.,
the field is public and non-final and, therefore, can be re-assigned by library users. The call-graph 
construction must assume that all possible subtypes of ```Type``` can be assigned to the field.
```java
// lib2/Demo.java
package lib2;

import lib.annotations.callgraph.CallSite;

public class Demo {
    
    public Type field = new Subtype();
    
    @CallSite(name = "method", line = 12, resolvedTargets = {"Llib2/Type;", "Llib2/Subtype;"}, 
    prohibitedTargets = "Llib2/SomeType;")
    public void callOnField(){
        field.method();
    }
}
```
```java
// lib2/Type.java
package lib2;

public class Type {
    
    public void method(){
        /* do something */
    }
}
```
```java
// lib2/Subtype.java
package lib2;

public class Subtype extends Type {
    
    public void method(){
        /* do something */
    }
}
```
```java
// lib2/SomeType.java
package lib2;

public class SomeType {
    
    public void method(){
        /* do something */
    }
}
```
[//]: # (END)

##LIB3
[//]: # (LIBRARY)
Tests library interface invocation for CBS edges under the following circumstances:
1) a ```public class PotentialSuperclass``` that can be inherited,
1) a ```public class DismissedSuperclass``` that cannot be inherited and, therefore, can't be target,
1) a ```public interface``` that can be inherited,
1) all of the previous mentioned classes/interfaces declare the method ```public void method()```. 
```java
// lib3/Demo.java
package lib3;

import lib.annotations.callgraph.CallSite;

public class Demo {
    
    @CallSite(name = "method", line = 10, resolvedTargets = "Llib3/PotentialSuperclass;",
    prohibitedTargets = "Llib3/DismissedSuperlass;")
    public static void libraryCallSite(Interface i){
        i.method();
    }
}
```
```java
// lib3/PotentialSuperclass.java
package lib3;

public class PotentialSuperclass {
    
    public void method() {
        
    }
}
```
```java
// lib3/DismissedSuperlass.java
package lib3;

public final class DismissedSuperlass {
    
    public void method() {
        
    }
}
```
```java
// lib3/Interface.java
package lib3;

public interface Interface {
    
    void method();
}
```
[//]: # (END)

##LIB4
[//]: # (LIBRARY)
Tests library interface invocation for CBS edges under the following circumstances:
1) a ```package visible class PotentialSuperclass``` in package ```lib4.collude``` that can be
inherited from a class within the same package, i.e. when a new class is added to the same package,
2) a ```package visible class InternalClass``` in package ```lib4.internal``` that can be inherited 
(analogously to 1) ),
3) a ```package visible interface``` in package ```lib4.collude``` that can be inherited from classes in the same package,
4) all of the previous mentioned classes/interfaces declare the method ```public void method()```. 
```java
// lib4/collude/Demo.java
package lib4.collude;

import lib.annotations.callgraph.CallSite;

public class Demo {
    
    @CallSite(name = "method", line = 10, resolvedTargets = "Llib4/collude/PotentialSuperclass;", 
    prohibitedTargets = "Llib4/internal/InternalClass;")
    public static void interfaceCallSite(PotentialInterface pi){
        pi.method();
    }
}
```
```java
// lib4/collude/PotentialSuperclass.java
package lib4.collude;

class PotentialSuperclass {
    
    public void method(){
        /* do something */
    }
}
```
```java
// lib4/collude/PotentialInterface.java
package lib4.collude;

interface PotentialInterface {
    
    void method();
}
```
```java
// lib4/internal/InternalClass.java
package lib4.internal;

class InternalClass {
    
    public void method(){
        /* do something */
    }
}
```
[//]: # (END)

##LIB5
[//]: # (LIBRARY)
Tests library interface invocation for CBS edges under the following circumstances:
1) a ```public class PotentialSuperclass``` in package ```lib5.internal``` that can be
inherited from and, therefore, provides the method ```public void method()``` from its superclass,
2) a ```package visible class InternalClass``` in package ```lib5.internal``` that can be inherited 
(analogously to 1) ),
3) a ```package visible interface``` in package ```lib5.collude``` that can be inherited from classes in the same package,
4) all of the previous mentioned classes/interfaces declare the method ```public void method()```. 
```java
// lib5/collude/Demo.java
package lib5.collude;

import lib.annotations.callgraph.CallSite;

public class Demo {
    
    @CallSite(name = "method", line = 9, resolvedTargets = "Llib5/internal/InternalClass;")
    public static void interfaceCallSite(PotentialInterface pi){
        pi.method();
    }
}

interface PotentialInterface {
    
    void method();
}
```
```java
// lib5/internal/PotentialSuperclass.java
package lib5.internal;

public class PotentialSuperclass extends InternalClass {
    
}

class InternalClass {
    
    public void method(){
        /* do something */
    }
}
```
[//]: # (END)