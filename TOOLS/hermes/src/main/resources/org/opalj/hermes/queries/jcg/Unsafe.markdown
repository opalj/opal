#Usage of `sun.misc.Unsafe`
Test cases related to the usage of `sun.misc.Unsafe`. This API provided methods to manipulate
the content of fields.

##Unsafe1
[//]: # (MAIN: unsafe.Demo)
Using ```Unsafe.compareAndSwapObject``` to set the value in a private field.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private TargetInterface objectVar = null;

	@DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 22)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        unsafe.compareAndSwapObject(o, objectOffset, null, new UnsafeTarget());
        o.objectVar.targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe2
[//]: # (MAIN: unsafe.Demo)
Using ```Unsafe.putObject``` to set the value in a private field.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private TargetInterface objectVar = null;

	@DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 22)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        unsafe.putObject(o, objectOffset, new UnsafeTarget());
        o.objectVar.targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe3
[//]: # (MAIN: unsafe.Demo)
Using ```Unsafe.getObject``` to retrieve a value of a private field.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private Object objectVar = null;

	@DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 23)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        o.objectVar = new UnsafeTarget();
        TargetInterface f = (TargetInterface) unsafe.getObject(o, objectOffset);
        f.targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe4
[//]: # (MAIN: unsafe.Demo)
Here, ```Unsafe.getAndSetObject``` is used to retrieve an object from a field and set a new object in parallel. 
Using getObject to retrieve a value of a private field.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;
import lib.annotations.callgraph.DirectCalls;

public class Demo {
    private Object objectVar = null;
    
    @DirectCalls({
	    @DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 29),
        @DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/SafeTarget;", returnType = String.class, line = 30)
    })
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo demo = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        demo.objectVar = new SafeTarget();
        UnsafeTarget unsafeTarget = new UnsafeTarget();
        TargetInterface f = (TargetInterface) unsafe.getAndSetObject(demo, objectOffset, unsafeTarget);
        
        ((TargetInterface)demo.objectVar).targetMethod();
        f.targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe5
[//]: # (MAIN: unsafe.Demo)
Here, ```Unsafe.putOrderedObject``` is used to put an object into a field. A call on that field must
be resolved to the newly written object.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private Object objectVar = null;
    
    @DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 25)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo demo = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        demo.objectVar = new SafeTarget();
        UnsafeTarget unsafeTarget = new UnsafeTarget();
        unsafe.putOrderedObject(demo, objectOffset, unsafeTarget);
        
        ((TargetInterface)demo.objectVar).targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe6
[//]: # (MAIN: unsafe.Demo)
Here, ```Unsafe.getObjectVolatile``` is used to retrieve an object from a class' field. The retrieved
object is then used to call ```toString``` on it.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private Object objectVar = null;
    
    @DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/SafeTarget;", returnType = String.class, line = 24)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo demo = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        demo.objectVar = new SafeTarget();
        TargetInterface target = (TargetInterface) unsafe.getObjectVolatile(demo, objectOffset);
        
        target.targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)

##Unsafe7
[//]: # (MAIN: unsafe.Demo)
Here, ```Unsafe.putObjectVolatile``` is used to write an object to a class' field. The written
object is then used to call ```toString``` on it.

```java
// unsafe/Demo.java
package unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import lib.annotations.callgraph.DirectCall;

public class Demo {
    private Object objectVar = null;
    
    @DirectCall(name = "targetMethod", resolvedTargets = "Lunsafe/UnsafeTarget;", returnType = String.class, line = 25)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo demo = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        demo.objectVar = new SafeTarget();
        UnsafeTarget unsafeTarget = new UnsafeTarget();
        unsafe.putObjectVolatile(demo, objectOffset, unsafeTarget);
        
        ((TargetInterface)demo.objectVar).targetMethod();
    }
}

interface TargetInterface {
    String targetMethod();
}

class UnsafeTarget implements TargetInterface{
	public String targetMethod() {
		return "UnsafeTarget";
	}
}

class SafeTarget implements TargetInterface {
    public String targetMethod() {
        return "SafeTarget";
    }
}
```
[//]: # (END)