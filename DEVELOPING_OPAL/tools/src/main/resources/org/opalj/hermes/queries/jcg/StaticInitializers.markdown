#StaticInitializers
Static initializers have to be treated as entry points.
##SI1
[//]: # (MAIN: si1.Bar)
A static initializer should be triggered when a non-constant field is referenced.

```java
// si1/Foo.java
package si1;

import lib.annotations.callgraph.CallSite;
public interface Foo {

	static String name = init();

    @CallSite(name = "callback", line = 10, resolvedTargets = "Lsi1/Foo;")
	static String init() {
		callback();
		return "Foo";
	}

	static void callback() {}
}
class Bar implements Foo {
	public static void main(String[] args) {
		Foo.name.toString();
	}
}
```
[//]: # (END)

##SI2
[//]: # (MAIN: si2.Bar)
Static initializer of an interface with a default method.

```java
// si2/Foo.java
package si2;

import lib.annotations.callgraph.CallSite;
public interface Foo {

	static String name = init();

    @CallSite(name = "callback", line = 10, resolvedTargets = "Lsi2/Foo;")
	static String init() {
		callback();
		return "Foo";
	}

	default String m() { return "Foo"; }

	static void callback() {}
}
class Bar implements Foo {
	public static void main(String[] args) {
		new Bar();
	}
}
```
[//]: # (END)

##SI3
[//]: # (MAIN: si3.Class)
An interface static initializer should be triggered when a static field with a non-primitive type
and non-String type is referenced.

```java
// si3/Class.java
package si3;

import lib.annotations.callgraph.CallSite;
public class Class {
	public static void main(String[] args) {
		Interface.referenceMe.toString();
	}
}

interface Interface {
    
    static String name = init();
    
    static Class referenceMe = new Class();
    
    @CallSite(name = "callback", line = 18, resolvedTargets = "Lsi3/Interface;")
    static String init() {
        callback();
        return "Interface";
    }
    
    static void callback(){}    
}
```
[//]: # (END)

##SI4
[//]: # (MAIN: si4.Foo)
Static initializer block of a class.

```java
// si4/Foo.java
package si4;

import lib.annotations.callgraph.CallSite;
public class Foo {

	static {
		init();
	}

    @CallSite(name = "callback", line = 12, resolvedTargets = "Lsi4/Foo;")
	static void init() {
		callback();
	}

	static void callback() {}

	public static void main(String[] args) {
		new Foo();
	}
}
```
[//]: # (END)

##SI5
[//]: # (MAIN: si5.Foo)
Static initializer method call in declaration of a class.

```java
// si5/Foo.java
package si5;

import lib.annotations.callgraph.CallSite;
public class Foo {

	static String name = init();

    @CallSite(name = "callback", line = 10, resolvedTargets = "Lsi5/Foo;")
	static String init() {
		callback();
		return "Foo";
	}

	static void callback() {}

	public static void main(String[] args) {
		new Foo();
	}
}
```
[//]: # (END)

##SI6
[//]: # (MAIN: si6.Class)
When a class is initialized, its super classes are also initialized.

```java
// si6/Class.java
package si6;

import lib.annotations.callgraph.CallSite;
public class Class extends SuperClass {

	static String name = init();

    @CallSite(name = "callback", line = 10, resolvedTargets = "Lsi6/Class;")
	static String init() {
		callback();
		return "Class";
	}

	static void callback() {}

	public static void main(String[] args) {
		new Class();
	}
}

class SuperClass extends RootClass {
    
    static {
        superInit();
    }
    
    @CallSite(name = "callback", line = 29, resolvedTargets = "Lsi6/SuperClass;")
    static void superInit(){
        callback();
    }
    
    static void callback() {}
}

class RootClass {
    
    static { 
        rootInit();
    }
    
    @CallSite(name = "callback", line = 43, resolvedTargets = "Lsi6/RootClass;")
    static void rootInit(){
      callback();
}  
    
    static void callback() {}
}
```
[//]: # (END)