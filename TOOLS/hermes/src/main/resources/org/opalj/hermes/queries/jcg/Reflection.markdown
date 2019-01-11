#TrivialReflection
This tests pertain to the Java's reflection API and comprise method call to
```java.lang.Class.getDeclaredMethod```, ```java.lang.Class.getMethod```,
```java.lang.Class.getField```, ```java.lang.reflect.Method.invoke```, and others. Cases that belong
to this category are rather trivially resolvable as all API inputs are directly known and neither
data-flow nor control-flow analyses are required. 

##TR1
[//]: # (MAIN: tr.Demo)
Tests the reflective call resolution when an object of type ```java.lang.Class``` is used to find a
declared method (```Class.getDeclaredMethod```) of this class. Afterwards, the found method is invoked.
Since the target method ```Demo.target``` is static and has no arguments, neither a receiver nor a
method argument is passed over the ```invoke``` method. 
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.IndirectCall;

class Demo {
    static String target() { return "42"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 13,
        resolvedTargets = "Ltr/Demo;"
    )
    public static void main(String[] args) throws Exception {
        Demo.class.getDeclaredMethod("target").invoke(null);
    }
}
```
[//]: # (END)

##TR2
[//]: # (MAIN: tr.Demo)
Tests the reflective call resolution when an object of type ```java.lang.Class``` is used to find a
declared method (```Class.getDeclaredMethod```) of this class. Afterwards, the found method is invoked.
Since the target method ```Demo.target``` is an instance method, the receiver (```this```) is passed
```invoke``` such that the method can be called on the actual receiver.
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.IndirectCall;

class Demo {
    public String target() { return "Demo"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 13,
        resolvedTargets = "Ltr/Demo;"
    )
    void caller() throws Exception {
        Demo.class.getDeclaredMethod("target").invoke(this);
    }

    public static void main(String[] args) throws Exception {
        new Demo().caller();
    }
}
```
[//]: # (END)

##TR3
[//]: # (MAIN: tr.Demo)
Tests the reflective call resolution when an object of type ```java.lang.Class``` is used to find a
method (```Class.getMethod```) of this class. Afterwards, the found method is invoked.
Since the target method ```Demo.target``` is an instance method, the receiver (```this```) is passed
```invoke``` such that the method can be called on the actual receiver.
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.IndirectCall;

public class Demo {
    public String target() { return "Demo"; }

    @IndirectCall(
        name = "target", returnType = String.class, line = 13,
        resolvedTargets = "Ltr/Demo;"
    )
    void caller() throws Exception {
        Demo.class.getMethod("target").invoke(this);
    }

    public static void main(String[] args) throws Exception { new Demo().caller(); }
}
```
[//]: # (END)

##TR4
[//]: # (MAIN: tr.Demo)
Tests the reflective call resolution when an object of type ```java.lang.Class``` is used to find a
declared method (```Class.getDeclaredMethod```) of this class. Afterwards, the found method is invoked.
Since the target method ```Demo.target``` is static and has one parameter, a ```null``` receiver as
well as a ```String``` matching the method's parameters are passed over the ```invoke``` method. 
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.IndirectCall;

class Demo {
    public static String target(String parameter) { return "Value: " + parameter; }

    @IndirectCall(
        name = "target", returnType = String.class, parameterTypes = String.class, line = 13,
        resolvedTargets = "Ltr/Demo;"
    )
    public static void main(String[] args) throws Exception {
        Demo.class.getDeclaredMethod("target", String.class).invoke(null, "42");
    }
}
```
[//]: # (END)

##TR5
[//]: # (MAIN: tr.Demo)
Tests the reflective invocation of a constructor by retrieving ```Demo```'s default constructor via
calling ```newInstance``` on ```tr.Demo```'s class object. This call must be resolved to Demo's 
```<init>``` method.
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

    @DirectCall(name="verifyCall", line=9, resolvedTargets = "Ltr/Demo;")
    public Demo() { Demo.verifyCall(); }

    public static void main(String[] args) throws Exception {
        Demo.class.newInstance();
    }
}
```
[//]: # (END)

##TR6
[//]: # (MAIN: tr.Demo)
Tests the reflective invocation of a constructor by retrieving ```Demo```'s constructor with a 
String argument via ```java.lang.Class```'s ```getConstructor``` method and then calling ```newInstance```
of the returned ```java.lang.reflect.Constructor``` object. This call must be resolved to Demo's 
```<init>(String)``` method.
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

    @DirectCall(name="verifyCall", line=9, resolvedTargets = "Ltr/Demo;")
    public Demo(String s) { Demo.verifyCall(); }

    public static void main(String[] args) throws Exception {
        Demo.class.getConstructor(String.class).newInstance("42");
    }
}
```
[//]: # (END)

##TR7
[//]: # (MAIN: tr.Demo)
Tests a reflective method invocation that is performed on a class' private field that is retrieved via the
reflection API. In ```tr.Demo```'s main method a new ```tr.Demo``` object is created and an object
of type ```tr.CallTarget``` is assigned to its field. This field is then retrieved via the reflection
using ```java.lang.Class.getDeclaredField(<fieldName>)``` and the field's name, namely ```"field"```.
```java.lang.reflect.Field.get``` is then used to get the object stored within the field of the Demo
instance that has been created previously. Afterwards, the returned instance is used to call
the ```target``` method.
```java
// tr/Demo.java
package tr;

import java.lang.reflect.Field;
import lib.annotations.callgraph.IndirectCall;

public class Demo {
    private Target field;

    @IndirectCall(
        name = "target", line = 18, resolvedTargets = "Ltr/CallTarget;"
    )
    public static void main(String[] args) throws Exception {
        Demo demo = new Demo();
        demo.field = new CallTarget();

        Field field = Demo.class.getDeclaredField("field");
        Target target = (Target) field.get(demo);
        target.target();
    }
}

interface Target {
    void target();
}

class CallTarget implements Target {
    public void target(){ /* do something */ }
}

class NeverInstantiated implements Target {
    public void target(){ /* do something */ }
}
```
[//]: # (END)

##TR8
[//]: # (MAIN: tr.Demo)
Tests a reflective method invocation that is performed on a class' public field that is retrieved via the
reflection API. In ```tr.Demo```'s main method a new ```tr.Demo``` object is created and an object
of type ```tr.CallTarget``` is assigned to its field. This field is then retrieved via the reflection
using ```java.lang.Class.getField(<fieldName>)``` and the field's name, namely ```"field"```.
```java.lang.reflect.Field.get``` is then used to get the object stored within the field of the Demo
instance that has been created previously. Afterwards, the returned instance is used to call
the ```target``` method.

```java
// tr/Demo.java
package tr;

import java.lang.reflect.Field;
import lib.annotations.callgraph.IndirectCall;

public class Demo {
    public Target field;

    @IndirectCall(
        name = "target", line = 18, resolvedTargets = "Ltr/CallTarget;"
    )
    public static void main(String[] args) throws Exception {
        Demo demo = new Demo();
        demo.field = new CallTarget();

        Field field = Demo.class.getField("field");
        Target t = (Target) field.get(demo);
        t.target();
    }
}

interface Target {
    void target();
}

class CallTarget implements Target {
    public void target(){ /* do something */ }
}

class NeverInstantiated implements Target {
    public void target(){ /* do something */ }
}
```
[//]: # (END)

##TR9
[//]: # (MAIN: tr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```tr.Demo``` ```Class.forName``` is called trying to retrieve an object of
```java.lang.Class``` which is parameterized over ```tr.InitializedClass```. This lookup can trigger
the static initializer of ```tr.InitializedClass``` which must thus be contained in program's call graph.
```java
// tr/Demo.java
package tr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

    public static void main(String[] args) throws Exception {
        Class.forName("tr.InitializedClass");
    }
}

class InitializedClass {
    
    static {
        staticInitializerCalled();
    }

    @DirectCall(name="verifyCall", line=21, resolvedTargets = "Ltr/Demo;")
    static private void staticInitializerCalled(){
        Demo.verifyCall();
    }
}
```
[//]: # (END)

#LocallyResolvableReflection
The complete information is locally (intra-procedurally) available.
##LRR1
[//]: # (MAIN: lrr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```lrr.Demo``` ```Class.forName``` is called trying to retrieve an object of
```java.lang.Class``` which is either parameterized over ```lrr.Left``` or ```lrr.Right```. This
lookup can trigger the static initializer of ```lrr.Left``` or ```lrr.Right``` which must thus be
contained in program's call graph.
```java
// lrr/Demo.java
package lrr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

    public static void main(String[] args) throws Exception {
        String className = (args.length % 2 == 0) ? "lrr.Left" : "lrr.Right"; 
        Class.forName(className);
    }
}

class Left {
    
    static {
        staticInitializerCalled();
    }

    @DirectCall(name="verifyCall", line=22, resolvedTargets = "Llrr/Demo;")
    static private void staticInitializerCalled(){
        Demo.verifyCall();
    }
}


class Right {
    
    static {
        staticInitializerCalled();
    }

    @DirectCall(name="verifyCall", line=35, resolvedTargets = "Llrr/Demo;")
    static private void staticInitializerCalled(){
        Demo.verifyCall();
    }
}
```
[//]: # (END)

##LRR2
[//]: # (MAIN: lrr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```lrr.Demo``` ```Class.forName``` is called trying to retrieve an object of
```java.lang.Class``` which is either parameterized over ```lrr.IsEven``` or ```lrr.IsOdd``` where 
both strings are constructed over a StringBuilder. This lookup can then trigger the static initializers
of ```lrr.IsEven``` or ```lrr.IsOdd``` which must thus be contained in program's call graph.
```java
// lrr/Demo.java
package lrr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

    public static void main(String[] args) throws Exception {
        StringBuilder builder = new StringBuilder("lrr.Is");
        if (args.length % 2 == 0)
            builder.append("Even"); 
        else
            builder.append("Odd");
        String className = builder.toString();
        Class.forName(className);
    }
}

class IsEven {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=27, resolvedTargets = "Llrr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }


 class IsOdd {

     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=40, resolvedTargets = "Llrr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

##LRR3
[//]: # (MAIN: lrr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```lrr.Demo``` ```Class.forName``` is called trying to retrieve an object of
```java.lang.Class``` which is parameterized over ```lrr.TargetClass```. The target String which is
passed to the respective ```Class.forName``` call is first assigned to Demo's field and then the
field's value is read and finally passed as parameter.
This lookup can then trigger the static initializer of ```lrr.TargetClass``` which must thus be
contained in program's call graph.
```java
// lrr/Demo.java
package lrr;

import lib.annotations.callgraph.DirectCall;

class Demo {
    private String className;

    public static void verifyCall(){ /* do something */ }

    public static void main(String[] args) throws Exception {
        Demo demo = new Demo();
        demo.className = "lrr.TargetClass";
        Class.forName(demo.className);
    }
}

class TargetClass {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=25, resolvedTargets = "Llrr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

#ContextSensitiveReflection
The concrete strings require information about the context.

##CSR1
[//]: # (MAIN: csr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```csr.Demo``` a static method is called that receives the string constant
which is transitively passed to ```Class.forName``` and then tries to retrieve an object of
```java.lang.Class``` which is parameterized over ```csr.TargetClass```. To infer the parameter that
flows into ```Class.forName``` inter-procedural string tracking is required. This lookup can trigger
the static initializer of ```csr.TargetClass```.
```java
// csr/Demo.java
package csr;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public static void verifyCall(){ /* do something */ }

    static void callForName(String className) throws Exception {
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
        Demo.callForName("csr.TargetClass");
    }
}

class TargetClass {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=24, resolvedTargets = "Lcsr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

##CSR2
[//]: # (MAIN: csr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```csr.Demo``` a static method is called that receives the string constant
which is transitively passed to ```Class.forName``` and then tries to retrieve an object of
```java.lang.Class``` which is parameterized over an __unknown__ type. In this test it is impossible
to get any information about the retrieved typed and, therefore, all possible types must be considered
for a sound method resolution.
```java
// csr/Demo.java
package csr;

import lib.annotations.callgraph.DirectCall;
public class Demo {
    public static void verifyCall(){ /* do something */ }

    static void callForName(String className) throws Exception {
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
        Demo.callForName(args[0]);
    }
}

class TargetClass {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=24, resolvedTargets = "Lcsr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

##CSR3
[//]: # (MAIN: csr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method of ```csr.Demo``` a static method is called reads the value from a static field which is
first written write before the method call to ```Demo.callForName``` and then passed to ```Class.forName```.
```Class.forName``` then tries to retrieve an object of ```java.lang.Class``` which is parameterized
over ```csr.CallTarget```. In this test it is impossible to get any information about the retrieved
typed and, therefore, all possible types must be considered for a sound method resolution.
```java
// csr/Demo.java
package csr;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public static String className;

    public static void verifyCall(){ /* do something */ }

    static void callForName() throws Exception {
        Class.forName(Demo.className);
    }

    public static void main(String[] args) throws Exception {
        Demo.className = "csr.CallTarget";
        Demo.callForName();
    }
}

class CallTarget {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=27, resolvedTargets = "Lcsr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

##CSR4
[//]: # (MAIN: csr.Demo)
This test cases concerns the reflection API as well as a class' static initializer. Within the main
method the methods ```java.lang.System.getProperties``` and ```java.lang.System.setProperties``` are
used to add a ```className``` property with the value ```csr.TargetClass``` to the global system
properties and thus make it globally available throughout the program. Afterwards,
```csr.Demo.callForName``` is called that then uses ```java.lang.System.getProperty("className")```
to access the stored string which is passed to the ```Class.forName``` call. Modelling system
properties would help to resolve this case soundly and better precision.
```java
// csr/Demo.java
package csr;

import java.util.Properties;
import lib.annotations.callgraph.DirectCall;

class Demo {

    public static void verifyCall(){ /* do something */ }

    static void callForName() throws Exception {
    	String className = System.getProperty("className");
        Class.forName(className);
    }

    public static void main(String[] args) throws Exception {
		Properties props = System.getProperties();
		props.put("className", "csr.TargetClass");
		System.setProperties(props);
        Demo.callForName();
    }
}

class TargetClass {
    
     static {
         staticInitializerCalled();
     }

     @DirectCall(name="verifyCall", line=31, resolvedTargets = "Lcsr/Demo;")
     static private void staticInitializerCalled(){
         Demo.verifyCall();
     }
 }
```
[//]: # (END)

#ClassForNameExceptions
Test cases w.r.t. to classloading using ```Class.forName(className:String)``` and its respective
exceptions to test whether valid path might be ignored which leads to unsoundness.

##CFNE1
[//]: # (MAIN: cfne.Demo)
This test case targets a common try catch pattern when classes are loaded. An existing class is loaded
over ```Class.forName(...)```, instantiated and then casted to another class. Unfortunately, the class
that is instantiated is __incompatible__ with the cast such that the operation results in a
```ClassCastException```.
```java
// cfne/Demo.java
package cfne;

import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void verifyCall(){ /* do something */ }

    @DirectCall(name="verifyCall", line = 15, resolvedTargets = "Lcfne/Demo;")
	public static void main(String[] args){
	    try {
	        Class cls = Class.forName("cfne.DeceptiveClass");
	        LoadedClass lCls = (LoadedClass) cls.newInstance();
	    } catch(ClassCastException cce){
	        verifyCall();
	    } catch(ClassNotFoundException cnfe){
	        // DEAD CODE
	    } catch(Exception rest){
            // DEAD CODE
        }
	}
}

class DeceptiveClass {

}

class LoadedClass {

}
```
[//]: # (END)

##CFNE2
[//]: # (MAIN: cfne.Demo)
This test case targets a common try catch pattern when classes are loaded. An absent class is loaded
over ```Class.forName(...)```. Since the class __can't be found__ the operation results in a ```ClassNotFoundException```
which is handled in one of the catch blocks.
```java
// cfne/Demo.java
package cfne;

import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void verifyCall(){ /* do something */ }

    @DirectCall(name="verifyCall", line = 18, resolvedTargets = "Lcfne/Demo;")
	public static void main(String[] args){
	    try {
	        Class cls = Class.forName("cfne.CatchMeIfYouCan");
	        // DEAD CODE
	        LoadedClass lCls = (LoadedClass) cls.newInstance();
	    } catch(ClassCastException cce){
	        /* DEAD CODE */
	    } catch(ClassNotFoundException cnfe){
	        verifyCall();
	    } catch(Exception rest){
	        //DEAD CODE
	    }
	}
}

class LoadedClass {

}
```
[//]: # (END)

##CFNE3
[//]: # (MAIN: cfne.Demo)
This case targets a concerns not only loading of classes but also the execution of their
static initializer. When a class is loaded, its static initializer must be called.
```java
// cfne/Demo.java
package cfne;

import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void verifyCall(){ /* do something */ }

	public static void main(String[] args){
	    try {
	        Class cls = Class.forName("cfne.LoadedClass");
	        Object lCls = cls.newInstance();
	    } catch(ClassCastException cce){
	        // DEAD CODE
	    } catch(ClassNotFoundException cnfe){
	        // DEAD CODE
	    } catch(Exception rest){
            //DEAD CODE
        }
	}
}

class LoadedClass {

    static {
        staticInitializerCalled();
    }

    @DirectCall(name="verifyCall", line=31, resolvedTargets = "Lcfne/Demo;")
    static private void staticInitializerCalled(){
        Demo.verifyCall();
    }
}
```
[//]: # (END)

##CFNE4
[//]: # (MAIN: cfne.Demo)
This case targets a concerns not only loading of classes but also the execution of their
static initializer. When a class is loaded, its static initializer must be called. Also the static
initializers of potential super classes.
```java
// cfne/Demo.java
package cfne;

import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void verifyCall(){ /* do something */ }

	public static void main(String[] args){
	    try {
	        Class cls = Class.forName("cfne.LoadedClass");
	        Object lCls = cls.newInstance();
	    } catch(ClassCastException cce){
	        // DEAD CODE
	    } catch(ClassNotFoundException cnfe){
	        // DEAD CODE
	    } catch(Exception rest){
            //DEAD CODE
        }
	}
}

class LoadedClass extends RootClass {

}

class RootClass {

    static {
        staticInitializerCalled();
    }

    @DirectCall(name="verifyCall", line=35, resolvedTargets = "Lcfne/Demo;")
    static private void staticInitializerCalled(){
        Demo.verifyCall();
    }
}
```
[//]: # (END)
