#DynamicProxies
The `java.lang.reflect.InvocationHandler` enables the implementation of dynamic proxies. 
##DP1
[//]: # (MAIN: dp.Main)
Tests a simple proxy implementation.

```java
// dp/Foo.java
package dp;

public interface Foo { Object bar(Object obj); }
```

```java
// dp/FooImpl.java
package dp;

public class FooImpl implements Foo { 
	public Object bar(Object obj) {
		return obj;
	}	
}
```

```java
// dp/DebugProxy.java
package dp;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

public class DebugProxy implements InvocationHandler {
 private Object obj;

 public static Object newInstance(Object obj) {
   return Proxy.newProxyInstance(
     obj.getClass().getClassLoader(),obj.getClass().getInterfaces(),
     new DebugProxy(obj));
  }

 private DebugProxy(Object obj) { this.obj = obj; }

 public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
   System.out.println("before method " + m.getName());
   return m.invoke(obj, args);
 }
}
```

```java
// dp/Main.java
package dp;

import lib.annotations.callgraph.IndirectCall;

public class Main {
	@IndirectCall(
        name = "bar", returnType = String.class, parameterTypes = String.class, line = 12,
        resolvedTargets = "Ldp/FooImpl;"
    )
	public static void main(String[] args) {
		Foo foo = (Foo) DebugProxy.newInstance(new FooImpl());
		foo.bar(null);
	}
}
```
[//]: # (END)