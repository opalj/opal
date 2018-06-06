#TypeNarrowing
Using local information to get better type information
##SimpleCast
[//]: # (MAIN: simplecast.Demo)
Type narrowing due to previous cast.

```java
// simplecast/Demo.java
package simplecast;

import lib.annotations.callgraph.CallSite;
class Demo {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lsimplecast/Target;"
    )
    static void m(Object o) {
        Target b = (Target) o;
        b.toString();
    }

    public String toString() { return "Demo"; }
}
class Target {
  public String toString() { return "Target"; }
}

```
[//]: # (END)

##CastClassAPI
[//]: # (MAIN: castclassapi.Demo)
Type narrowing due to previous cast using java class API.

```java
// castclassapi/Demo.java
package castclassapi;

import lib.annotations.callgraph.CallSite;
class Demo {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lcastclassapi/Target;"
    )
    static void m(Object o) {
        Target target = Target.class.cast(o);
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

##ClassEQ
[//]: # (MAIN: classeq.Demo)
Type narrowing due to a class equality check. Within this branch it's known that that object ```o```
must be of type ```Target```.

```java
// classeq/Demo.java
package classeq;

import lib.annotations.callgraph.CallSite;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lclasseq/Target;"
    )
    static void m(Object o) {
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


##InstanceOf
[//]: # (MAIN: instanceofcheck.Demo)
Type narrowing due to previous instance of check.

```java
// instanceofcheck/Demo.java
package instanceofcheck;

import lib.annotations.callgraph.CallSite;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Linstanceofcheck/Target;"
    )
    static void m(Object o) {
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

##InstanceOfClassAPI
[//]: # (MAIN: instanceofclassapi.Demo)
Type narrowing due to previous instance of check.

```java
// instanceofclassapi/Demo.java
package instanceofclassapi;

import lib.annotations.callgraph.CallSite;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Linstanceofclassapi/Target;"
    )
    static void m(Object o) {
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


##IsAssignable
[//]: # (MAIN: isssignable.Demo)
Type narrowing due to previous is assignable.

```java
// isssignable/Demo.java
package isssignable;

import lib.annotations.callgraph.CallSite;
class Demo{ 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) 
          m(new Target());
        else 
          m(new Demo());
    }

    @CallSite(
        name = "toString", returnType = String.class, line = 18,
        resolvedTargets = "Lisssignable/Target;"
    )
    static void m(Object o) {
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