#TypeCasts
Using local information to get better type information. Even if those APIs and instructions do have
no visible effect on the soundness they still must be supported by the frameworks. 
##TC1
[//]: # (MAIN: simplecast.Demo)
This case shows type narrowing due to previous cast. The method ```simplecast.Demo.castToTarget``` takes an
object, casts it to ```simplecast.Target```, and then calls ```target``` on the casted object which
rules out ```simplecast.Demo.target``` as receiver.
```java
// simplecast/Demo.java
package simplecast;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          castToTarget(new Target());
        else 
          castToTarget(new Demo());
    }

    @DirectCall(
        name = "target", returnType = String.class, line = 18,
        resolvedTargets = "Lsimplecast/Target;"
    )
    static void castToTarget(Object o) {
        Target b = (Target) o;
        b.target();
    }

    public String target() { return "Demo"; }
}
class Target {
  public String target() { return "Target"; }
}
```
[//]: # (END)

##TC2
[//]: # (MAIN: castclassapi.Demo)
Type narrowing due to previous cast using Java's class API. The method ```castclassapi.Demo.castToTarget```
takes a class object that is parameterized over the type the cast should be performed to and an object
that will be casted within the method. It then casts it via ```Class.cast``` to ```castclassapi.Target```
and then calls ```toString``` on the casted object which rules out ```castclassapi.Demo.toString``` as receiver.
```java
// castclassapi/Demo.java
package castclassapi;

import lib.annotations.callgraph.DirectCall;
class Demo {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          castToTarget(Target.class, new Target());
        else 
          castToTarget(Demo.class, new Demo());
    }

    @DirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lcastclassapi/Target;"
    )
    static <T> void castToTarget(Class<T> cls,  Object o) {
        T target = cls.cast(o);
        target.toString();
    }

    public String toString() { return "Demo"; }
}
```
```java
// castclassapi/Target.java
package castclassapi;

public class Target {
    
    public String toString(){
        return "Target";
    }
}
```
[//]: # (END)

##TC3
[//]: # (MAIN: classeq.Demo)
Type narrowing due to a equality check of two ```java.lang.Class``` objects. Within the ```this```
branch of ```classeq.Demo.callIfInstanceOfTarget``` it is thus known that the passed object ```o```
must be of type ```Target```. Hence, ```o.toString``` must only be resolved to ```Target.toString```.
```java
// classeq/Demo.java
package classeq;

import lib.annotations.callgraph.DirectCall;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          callIfInstanceOfTarget(new Target());
        else 
          callIfInstanceOfTarget(new Demo());
    }

    @DirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lclasseq/Target;"
    )
    static void callIfInstanceOfTarget(Object o) {
      if (o.getClass() == Target.class)
        o.toString();
    }

    public String toString() { return "Demo"; }
}
class Target {
  public String toString() { return "Target"; }
}

```
[//]: # (END)


##TC4
[//]: # (MAIN: instanceofcheck.Demo)
Type narrowing due to Java's built-in ```instanceof``` check of the given object ```o``` and the 
```Target``` class. Within the ```this``` branch of ```Demo.callIfInstanceOfTarget``` it is thus
known that the passed object ```o``` must be of type ```Target```. Hence, ```o.toString``` must only
be resolved to ```Target.toString```.
```java
// instanceofcheck/Demo.java
package instanceofcheck;

import lib.annotations.callgraph.DirectCall;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          callIfInstanceOfTarget(new Target());
        else 
          callIfInstanceOfTarget(new Demo());
    }

    @DirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Linstanceofcheck/Target;"
    )
    static void callIfInstanceOfTarget(Object o) {
      if (o instanceof Target)
        o.toString();
    }

    public String toString() { return "Demo"; }
}
class Target {
  public String toString() { return "Target"; }
}

```
[//]: # (END)

##TC5
[//]: # (MAIN: instanceofclassapi.Demo)
Type narrowing due to Java's ```java.lang.Class.isInstance``` API call that checks whether a given
object (i.e. ```o```) is of the same type the class instance is parameterized over, i.e.,
```Target.class``` is a shorthand notation for ```java.lang.Class<Target>```. Within the ```this```
branch of ```Demo.callIfInstanceOfTarget``` it is thus known that the passed object ```o``` must be
of type ```Target```. Hence, ```o.toString``` must only be resolved to ```Target.toString```.
```java
// instanceofclassapi/Demo.java
package instanceofclassapi;

import lib.annotations.callgraph.DirectCall;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          callIfInstanceOfTarget(new Target());
        else 
          callIfInstanceOfTarget(new Demo());
    }

    @DirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Linstanceofclassapi/Target;"
    )
    static void callIfInstanceOfTarget(Object o) {
      if (Target.class.isInstance(o))
        o.toString();
    }

    public String toString() { return "Demo"; }
}
class Target {
  public String toString() { return "Target"; }
}

```
[//]: # (END)


##TC6
[//]: # (MAIN: tc.Demo)
Type narrowing due to Java's ```java.lang.Class.isAssignableFrom``` API call that checks whether a given
object (i.e. ```o```) can be assign to variable of the type the class instance is parameterized over, i.e.,
```Target.class``` is a shorthand notation for ```java.lang.Class<Target>```. Within the ```this```
branch of ```Demo.callIfInstanceOfTarget``` it is thus known that the passed object ```o``` must be
a subtype of ```Target```. Hence, ```o.toString``` must only be resolved to ```Target.toString```.
```java
// tc/Demo.java
package tc;

import lib.annotations.callgraph.DirectCall;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          callIfInstanceOfTarget(new Target());
        else 
          callIfInstanceOfTarget(new Demo());
    }

    @DirectCall(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Ltc/Target;"
    )
    static void callIfInstanceOfTarget(Object o) {
      if (Target.class.isAssignableFrom(o.getClass()))
        o.toString();
    }

    public String toString() { return "Demo"; }
}
class Target {
  public String toString() { return "Target"; }
}

```
[//]: # (END)