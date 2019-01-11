#StaticInitializers
Static initializers have to be treated as (on-the-fly) entry points as they are called by the JVM
when a class is loaded. More details can be found in the JVM spec (§12.4.1 - When initialization occurs). 
##SI1
[//]: # (MAIN: si.Main)
A static initializer should be triggered when a *non-constant static field* is referenced. The scenario below
shows an interface ```si.NonConstantFieldRef``` which declares a static non-constant field that is referenced
once in the ```si.Main```'s main method. When the field is references, the field must be initialized
and in order to do so, the JVM calls ```si.NonConstantFieldRef```'s static initializer (```<clinit>```). 
```java
// si/NonConstantFieldRef.java
package si;

import lib.annotations.callgraph.DirectCall;
public interface NonConstantFieldRef {

	static String nonConstantField = init();

    @DirectCall(name = "callback", line = 10, resolvedTargets = "Lsi/Demo;")
	static String init() {
		callback();
		return "Demo";
	}

	static void callback() {}
}

class Main {
	public static void main(String[] args) {
		NonConstantFieldRef.nonConstantField.toString();
	}
}
```
[//]: # (END)

##SI2
[//]: # (MAIN: si.Demo)
A static initializer should be triggered when a *static interface method* is invoked. The scenario below
shows an interface ```si.Interface``` which declares a static method (```callback```) which is called in the
```si.Demo```'s main method. The invocation of ```callback``` causes the JVM to call ```si.Interface```'s
 static initializer (```<clinit>```). 
 >Please not that this is not directly annotatable and we thus use a static initialized field that
 is also initialized within the static initializer and triggers a method invocation that can be tested. 
```java
// si/Interface.java
package si;

import lib.annotations.callgraph.DirectCall;
public interface Interface {

	static String name = init();

    @DirectCall(name = "callback", line = 10, resolvedTargets = "Lsi/Interface;")
	static String init() {
		callback();
		return "Demo";
	}

	static void callback() {}
}
class Demo {
    
	public static void main(String[] args) {
		Interface.callback();
	}
}
```
[//]: # (END)

##SI3
[//]: # (MAIN: si.Demo)
Static initializer of an interface with a default method. An interface's static initializer is also
triggered when 1) *a subtype is initialized* and 2) *the interface has a default method*. Where as 1)
is given when a new instance of ```si.Demo``` - it implements the interface ```si.Interface```- is
created  in ```si.Demo```'s main method, 2) is given because ```si.Interface``` declares the
default method ```Interface.defaultMethod```. Since both criteria are fulfilled, the JVM will also
initialize ```si.Interface```.
```java
// si/Interface.java
package si;

import lib.annotations.callgraph.DirectCall;
public interface Interface {

	static String name = init();

    @DirectCall(name = "callback", line = 10, resolvedTargets = "Lsi/Interface;")
	static String init() {
		callback();
		return "Demo";
	}

	default String defaultMethod() { return "Demo"; }

	static void callback() {}
}
class Demo implements Interface {
	public static void main(String[] args) {
		new Demo();
	}
}
```
[//]: # (END)

##SI4
[//]: # (MAIN: si.Demo)
An interface static initializer should be triggered when a *final static field* with a *non-primitive type*
and *non-String type* is referenced. The scenario below shows an interface ```si.Interface``` which
declares a static final field of type ```si.Demo``` that is referenced once in the ```si.Demo```'s
main method. When the field is referenced, the field must be initialized and in order to do so,
the JVM calls ```si.Interface```'s static initializer (```<clinit>```). 
```java
// si/Demo.java
package si;

import lib.annotations.callgraph.DirectCall;
public class Demo {
	public static void main(String[] args) {
		Interface.referenceMe.toString();
	}
}

interface Interface {

    static String testHook = init();
    static final Demo referenceMe = new Demo();

    @DirectCall(name = "callback", line = 17, resolvedTargets = "Lsi/Interface;")
    static String init() {
        callback();
        return "Interface";
    }

    static void callback(){}
}
```
[//]: # (END)

##SI5
[//]: # (MAIN: si.Main)
The instantiation of a class should trigger its static initializer. The class ```si.Demo``` has an
explictly defined static initializer. When a new instance of the ```si.Demo``` class is created in
```si.Main```'s main method, the static initializer of ```si.Demo``` must be executed. 
```java
// si/Main.java
package si;

import lib.annotations.callgraph.DirectCall;
public class Main{

	public static void main(String[] args) {
		new Demo();
	}
}

class Demo {

	static {
		init();
	}

    @DirectCall(name = "callback", line = 19, resolvedTargets = "Lsi/Demo;")
	static void init() {
		callback();
	}

	static void callback() {}
}
```
[//]: # (END)

##SI6
[//]: # (MAIN: si.Main)
```si.Demo``` declares a static method which is called in ```si.Main```'s main method. Calling a
static method on a class receiver triggers the JVM to execute its static initializer when the class
was not used before.
```java
// si/Main.java
package si;

import lib.annotations.callgraph.DirectCall;
public class Main {

	public static void main(String[] args) {
		Demo.callback();
	}
}

class Demo {
	static String name = init();

    @DirectCall(name = "callback", line = 16, resolvedTargets = "Lsi/Demo;")
	static String init() {
		callback();
		return "42";
	}

	static void callback() {}
}
```
[//]: # (END)

##SI7
[//]: # (MAIN: si.Main)
Assigning a *non-final static field* (e.g. ```si.Demo.assignMe```) triggers the JVM to execute
```si.Demo``` static initializer. ```si.Demo.assignMe``` is assigned in ```si.Main```'s main method
and thus its static initializer must be contained in the call graph.
```java
// si/Main.java
package si;

import lib.annotations.callgraph.DirectCall;
public class Main {

	public static void main(String[] args) {
		Demo.assignMe = 42;
	}
}

class Demo {
	static String name = init();

    static int assignMe;

    @DirectCall(name = "callback", line = 18, resolvedTargets = "Lsi/Demo;")
	static String init() {
		callback();
		return "Demo";
	}

	static void callback() {}
}
```
[//]: # (END)

##SI8
[//]: # (MAIN: si.Main)
When initialization of a class occurs during execution, all its super classes must be initialized
beforehand. In ```si.Main```'s main method an instance of ```si.Subclass``` is created. Hence, all
super classes of ```si.Subclass``` must be initialized too. Those are ```si.Superclass``` as direct
super class and ```si.RootClass``` as transitive super class. Since all three classes provide static
initialization routines, calls to all must be included in the call graph.
```java
// si/Main.java
package si;

import lib.annotations.callgraph.DirectCall;
public class Main {

	public static void main(String[] args) {
		new Subclass();
	}
}

class Subclass extends Superclass {
	static String name = init();

    @DirectCall(name = "callback", line = 16, resolvedTargets = "Lsi/Subclass;")
	static String init() {
		callback();
		return "Subclass";
	}

	static void callback() {}
}

class Superclass extends RootClass {

    static {
        superInit();
    }

    @DirectCall(name = "callback", line = 31, resolvedTargets = "Lsi/Superclass;")
    static void superInit(){
        callback();
    }

    static void callback() {}
}

class RootClass {

    static {
        rootInit();
    }

    @DirectCall(name = "callback", line = 45, resolvedTargets = "Lsi/RootClass;")
    static void rootInit(){
      callback();
    }

    static void callback() {}
}
```
[//]: # (END)