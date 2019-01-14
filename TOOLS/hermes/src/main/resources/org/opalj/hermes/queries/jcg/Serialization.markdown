#SerializableClasses
These category comprises test cases that model callbacks that must be handled when dealing with 
```java.io.Serializable``` classes. As soon as object (de-)serialization is found within a program
those mechanism can be used and all related methods must therefore be considered as on-the-fly
entry points.
##Ser1
[//]: # (MAIN: ser.Demo)
This test pertains to the ```writeObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```writeObject``` forces
the JVM to call ```writeObject``` instead of the normally used ```defaultWriteObject``` method. In
```ser.Demo```'s main method, an instance of ```ser.Demo``` as well as an ```java.io.ObjectOutputStream```
is created. The latter is then used to serialize the instance of ```ser.Demo``` that has been created
previously. Serializing this object triggers the JVM which then calls the overridden 
```ser.Demo.writeObject``` method.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    @DirectCall(name = "defaultWriteObject", resolvedTargets = "Ljava/io/ObjectOutputStream;", line = 15)
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	out.defaultWriteObject();
    }

    public static void main(String[] args) throws Exception {
    	Demo serialize = new Demo();
    	FileOutputStream fos = new FileOutputStream("test.ser");
    	ObjectOutputStream out = new ObjectOutputStream(fos);
    	out.writeObject(serialize); // triggers serialization
    	out.close();
    }
}
```
[//]: # (END)

##Ser2
[//]: # (MAIN: ser.Demo)
This test pertains to the ```writeObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```writeObject``` forces
the JVM to call ```writeObject``` instead of the normally used ```defaultWriteObject``` method. In
```ser.Demo```'s main method, an instance of ```ser.Demo```, ```ser.AnotherSerializableClass```,
as well as an ```java.io.ObjectOutputStream``` is created. The latter is then used to serialize either
an instance of ```ser.Demo``` or ```ser.AnotherSerializableClass``` that might have been created
previously. Serializing this object triggers the JVM which then either calls the overridden 
```ser.Demo.writeObject``` method or the ```defaultWriteObject``` method when 
```ser.AnotherSerializableClass``` is serialized.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    @DirectCall(name = "defaultWriteObject", resolvedTargets = "Ljava/io/ObjectOutputStream;", line = 15)
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public static void main(String[] args) throws Exception {
        Object serialize;
        if(args.length == 0)
            serialize = new Demo();
        else
            serialize = new AnotherSerializableClass();
        FileOutputStream fos = new FileOutputStream("test.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(serialize);
        out.close();
    }
}

class AnotherSerializableClass implements Serializable {}
```
[//]: # (END)

##Ser3
[//]: # (MAIN: ser.Demo)
This test pertains to the ```writeObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```writeObject``` forces
the JVM to call ```writeObject``` instead of the normally used ```defaultWriteObject``` method. In
```ser.Demo```'s main method, an instance of ```ser.Demo``` is created and passed to static method.
This method, ```ser.Demo.serialize```, creates an output stream and serializes the object given by the method's parameter.
Without considering inter-procedural information, the test case can only be modeled imprecisely by
taking all ```writeObject``` methods into account.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    @DirectCall(name = "defaultWriteObject", resolvedTargets = "Ljava/io/ObjectOutputStream;", line = 15)
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public static void serialize(Object serialize) throws Exception {
        FileOutputStream fos = new FileOutputStream("test.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(serialize);
        out.close();
    }

    public static void main(String[] args) throws Exception {
        Demo serializeIt = new Demo();
        serialize(serializeIt);
    }
}
```
[//]: # (END)

##Ser4
[//]: # (MAIN: ser.Demo)
This test pertains to the ```readObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```readObject``` forces
the JVM to call ```readObject``` instead of the normally used ```defaultReadObject``` method. In
```ser.Demo```'s main method, an ```ObjectInputStream``` is used to deserialize a previously
serialized object from the ```test.ser``` file. The call to ```ObjectInputStream.readObject``` then
causes the JVM to trigger the deserialization mechanism which in turn calls ```ser.Demo.readObject```.
Without any information about the file's content it is impossible to resolve the call precisely.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    @DirectCall(name = "defaultReadObject", resolvedTargets = "Ljava/io/ObjectInputStream;", line = 15)
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Object obj = in.readObject();
        in.close();
    }
}
```
[//]: # (END)

##Ser5
[//]: # (MAIN: ser.Demo)
This test pertains to the ```readObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```readObject``` forces
the JVM to call ```readObject``` instead of the normally used ```defaultReadObject``` method. In
```ser.Demo```'s main method, an ```ObjectInputStream``` is used to deserialize a previously
serialized object from the ```test.ser``` file. The call to ```ObjectInputStream.readObject``` then
causes the JVM to trigger the deserialization mechanism which in turn calls ```ser.Demo.readObject```.
The returned result is then casted to ```ser.Demo``` which either results in a class cast exception or
implies that the deserialized object was from type ```ser.Demo```.
Without any information about the file's content it is impossible to resolve the call precisely.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    @DirectCall(name = "defaultReadObject", resolvedTargets = "Ljava/io/ObjectInputStream;", line = 15)
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

##Ser6
[//]: # (MAIN: ser.Demo)
This test pertains to the ```writeReplace``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```writeReplace``` forces
the JVM to call ```writeReplace``` instead of the normally used ```defaultwriteObject``` method. In
```ser.Demo```'s main method, an ```ObjectOutputStream``` is used to serialize an instance of
```ser.Demo```. The call to ```ObjectInputStream.writeObject``` then causes the JVM to trigger the
serialization mechanism which in turn calls ```ser.Demo.writeReplace```.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    public Object replace() { return this; }
	@DirectCall(name = "replace", returnType = Object.class, resolvedTargets = "Lser/Demo;", line = 17)
    private Object writeReplace() throws ObjectStreamException {
    	return replace();
    }

    public static void main(String[] args) throws Exception {
    	Demo serialize = new Demo();
    	FileOutputStream fos = new FileOutputStream("test.ser");
    	ObjectOutputStream out = new ObjectOutputStream(fos);
    	out.writeObject(serialize);
    	out.close();
    }
}
```
[//]: # (END)

##Ser7
[//]: # (MAIN: ser.Demo)
This test pertains to the ```readResolve``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```readResolve``` forces
the JVM to call ```readResolve``` instead of the normally used ```defaultReadObject``` method when 
```readObject``` is not overridden. In ```ser.Demo```'s main method, an ```ObjectOutputStream``` is
used to serialize an instance of ```ser.Demo```. The call to ```ObjectInputStream.readObject``` then
causes the JVM to trigger the serialization mechanism which in turn calls ```ser.Demo.readResolve```.
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    public Object replace() { return this; }
    @DirectCall(name = "replace", returnType = Object.class, resolvedTargets = "Lser/Demo;", line = 17)
    private Object readResolve() throws ObjectStreamException {
        return replace();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

##Ser8
[//]: # (MAIN: ser.Demo)
This test pertains to the ```validateObject``` callback method that can be implemented when a class
implements ```java.io.Serializable``` as ```ser.Demo``` does. Overriding ```validateObject``` implies
that it can be called by the JVM after an object is deserialized when a validation procedure has been
registered (see ```registerValidation``` in ```readObject```).
```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.ObjectInputValidation;
import java.io.InvalidObjectException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable, ObjectInputValidation {
    
    static final long serialVersionUID = 42L;
    
    public void callback() { }
    @DirectCall(name = "callback", resolvedTargets = "Lser/Demo;", line = 19)
    public void validateObject() throws InvalidObjectException {
        callback();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.registerValidation(this, 0);
        in.defaultReadObject();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

##Ser9
[//]: # (MAIN: ser.Demo)
This scenario tests whether the constructor calls w.r.t. serializable classes are handled soundly.
During deserialization, the JVM calls the first constructor that neither has any formal parameter nor
belongs to ```Serializable``` class of the class which is deserialized. In the scenario below, an
instance of ```ser.Demo``` is going to be deserialized and during this process ```Superclass.<init>```
is called.
```java
// ser/Superclass.java
package ser;

import lib.annotations.callgraph.DirectCall;

public class Superclass {
    public void callback() { }

    @DirectCall(name = "callback", resolvedTargets = "Lser/Superclass;", line = 10)
    public Superclass() {
        callback();
    }
}
```

```java
// ser/Demo.java
package ser;

import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

public class Demo extends Superclass implements Serializable {
    
    static final long serialVersionUID = 42L;
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

#ExternalizableClasses
Callback methods related to ```java.io.Externalizable``` classes.
##ExtSer1
[//]: # (MAIN: extser.Demo)
This test pertains to the ```writeExternal``` callback method that can be implemented when a class
implements ```java.io.Externalizable``` as ```ser.Demo``` does. Overriding ```writeExternal``` forces
the JVM to call ```writeExternal``` during an object's serialization via ```Externalizable```. In
```ser.Demo```'s main method, an instance of ```ser.Demo``` as well as an ```java.io.ObjectOutputStream```
is created. The latter is then used to serialize the instance of ```ser.Demo``` that has been created
previously. Serializing this object triggers the JVM's serialization mechanism  which then calls the
overridden ```ser.Demo.writeExternal``` method.
```java
// extser/Demo.java
package extser;

import java.io.Externalizable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Externalizable {

    @DirectCall(name = "callback", resolvedTargets = "Lextser/Demo;", line = 17)
    public void writeExternal(ObjectOutput out) throws IOException {
        callback();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        callback();
    }

    public void callback() { }

    public static void main(String[] args) throws Exception {
        Demo f = new Demo();
        FileOutputStream fos = new FileOutputStream("test.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(f);
        out.close();
    }
}
```
[//]: # (END)

##ExtSer2
[//]: # (MAIN: extser.Demo)
This test pertains to the ```readExternal``` callback method that can be implemented when a class
implements ```java.io.Externalizable``` as ```ser.Demo``` does. Overriding ```readExternal``` forces
the JVM to call ```readExternal``` during an object's deserialization via ```Externalizable```. In
```ser.Demo```'s main method, an instance of ```ser.Demo``` as well as an ```java.io.ObjectInputStream```
is created. The latter is then used to deserialize the instance of ```ser.Demo``` that was written to
```test.ser```previously. Deserializing this object triggers the JVM's deserialization mechanism 
which then calls the overridden ```ser.Demo.readExternal``` method.
```java
// extser/Demo.java
package extser;

import java.io.Externalizable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Externalizable {
    
    @DirectCall(name = "callback", resolvedTargets = "Lextser/Demo;", line = 17)
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        callback();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        callback();
    }

    public void callback() { }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

##ExtSer3
[//]: # (MAIN: extser.Demo)
This scenario tests whether the constructor calls w.r.t. externalizable classes are handled soundly.
During deserialization, the JVM calls the no-argument constructor of the ```Externalizable``` class.
```java
// extser/Demo.java
package extser;

import java.io.Externalizable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.IOException;
import lib.annotations.callgraph.DirectCall;

public class Demo implements Externalizable {
    
    public void callback() { }

    @DirectCall(name = "callback", resolvedTargets = "Lextser/Demo;", line = 19)
    public Demo() {
        callback();
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        callback();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        callback();
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("test.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Demo obj = (Demo) in.readObject();
        in.close();
    }
}
```
[//]: # (END)

#Serialization and Lambdas
Tests Java's serialization mechanism when Lambdas are (de)serialized, i.e., de(serialization) of Lambdas
causes the JVM to use ```java.lang.invoke.SerializedLambda```.
##SerLam1
[//]: # (MAIN: serlam.DoSerialization)
Tests whether the serialization of lambdas that implement a functional interface is modelled correctly.
```java
// serlam/DoSerialization.java
package serlam;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import lib.annotations.callgraph.IndirectCall;

public class DoSerialization {

    @FunctionalInterface interface Test extends Serializable{
        String concat(Integer seconds);
    }
    
    @IndirectCall(
            name = "writeReplace",
            line = 33,
            resolvedTargets = "Ljava/lang/invoke/SerializedLambda;")
    public static void main(String[] args) throws Exception {
        float y = 3.13f;
        String s = "bar";
        
        Test lambda = (Integer x) -> "Hello World " + x + y + s;
        
        FileOutputStream fos = new FileOutputStream("serlam1.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(lambda);
        out.close();
        
        
    }
}
```
[//]: # (END)

##SerLam2
[//]: # (MAIN: serlam.DoDeserialization)
Tests whether the deserialization of lambdas that implement a functional interface is modelled correctly.
```java
// serlam/DoSerialization.java
package serlam;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class DoSerialization {

    public static void main(String[] args) throws Exception {
        float y = 3.14f;
        String s = "foo";
        
        Test lambda = (Integer x) -> "Hello World " + x + y + s;
        
        FileOutputStream fos = new FileOutputStream("serlam2.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(lambda);
        out.close();
    }
}
```
```java
// serlam/Test.java
package serlam;

import java.io.Serializable;

public @FunctionalInterface interface Test extends Serializable{
    String concat(Integer seconds);
}
```

```java
// serlam/DoDeserialization.java
package serlam;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import lib.annotations.callgraph.IndirectCall;

public class DoDeserialization {

    @IndirectCall(
            name = "readResolve",
            line = 18,
            resolvedTargets = "Ljava/lang/invoke/SerializedLambda;")
    public static void main(String[] args) throws Exception {
        DoSerialization.main(args);
        FileInputStream fis = new FileInputStream("serlam2.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        Object obj = in.readObject();
        in.close();
    }
}
```
[//]: # (END)