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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;

	@CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 22)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        unsafe.compareAndSwapObject(o, objectOffset, null, new UnsafeTarget());
        o.objectVar.toString();
    }
}

class UnsafeTarget {
	public String toString() {
		return "UnsafeTarget";
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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;

	@CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 22)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        unsafe.putObject(o, objectOffset, new UnsafeTarget());
        o.objectVar.toString();
    }
}

class UnsafeTarget {
	public String toString() {
		return "UnsafeTarget";
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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;

	@CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 23)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo o = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        o.objectVar = new UnsafeTarget();
        Object f = unsafe.getObject(o, objectOffset);
        f.toString();
    }
}

class UnsafeTarget {
	public String toString() {
		return "UnsafeTarget";
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
import lib.annotations.callgraph.CallSite;
import lib.annotations.callgraph.CallSites;

public class Demo {
    private Object objectVar = null;
    
    @CallSites({
	    @CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 29),
        @CallSite(name = "toString", resolvedTargets = "Lunsafe/SafeTarget;", line = 30)
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
        Object f = unsafe.getAndSetObject(demo, objectOffset, unsafeTarget);
        
        demo.objectVar.toString();
        f.toString();
    }
}

class UnsafeTarget {
    
	public String toString() {
		return UnsafeTarget.class.toString();
	}
}

class SafeTarget {
    
	public String toString() {
		return SafeTarget.class.toString();
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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;
    
    @CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 25)
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
        
        demo.objectVar.toString();
    }
}

class UnsafeTarget {
    
	public String toString() {
		return UnsafeTarget.class.toString();
	}
}

class SafeTarget {
    
	public String toString() {
		return SafeTarget.class.toString();
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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;
    
    @CallSite(name = "toString", resolvedTargets = "Lunsafe/SafeTarget;", line = 24)
    public static void main(String[] args) throws Exception {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        Demo demo = new Demo();
        Field objectField = Demo.class.getDeclaredField("objectVar");
        long objectOffset = unsafe.objectFieldOffset(objectField);

        demo.objectVar = new SafeTarget();
        Object target = unsafe.getObjectVolatile(demo, objectOffset);
        
        target.toString();
    }
}

class UnsafeTarget {
    
	public String toString() {
		return UnsafeTarget.class.toString();
	}
}

class SafeTarget {
    
	public String toString() {
		return SafeTarget.class.toString();
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
import lib.annotations.callgraph.CallSite;

public class Demo {
    private Object objectVar = null;
    
    @CallSite(name = "toString", resolvedTargets = "Lunsafe/UnsafeTarget;", line = 25)
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
        
        demo.objectVar.toString();
    }
}

class UnsafeTarget {
    
	public String toString() {
		return UnsafeTarget.class.toString();
	}
}

class SafeTarget {
    
	public String toString() {
		return SafeTarget.class.toString();
	}
}
```
[//]: # (END)