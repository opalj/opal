#JVMCalls
JVM calls or callbacks must be treated as (on-the-fly) entry points and explicitly modelled for correct
call-graph construction, i.e., when certain operations are performed like creating an object or 
adding an ```ShutdownHook```. 

Please note that Java's Serialization feature is a similar mechanism. However, Serialization is a
substantial feature and is thus handled in a separate category.

##JVMC1
[//]: # (MAIN: jvmc.Demo)
This tests covers a callback that can be introduced to the program by calling```Runtime.addShutdownHook```.
It allows the program to pass a customizable thread to the JVM that is called on the JVM's shut down. 
```java
// jvmc/Demo.java
package jvmc;

import java.lang.System;
import java.lang.Runtime;

import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void callback(){ /* do something */ }

	public static void main(String[] args){
        Runnable r = new TargetRunnable();
        Runtime.getRuntime().addShutdownHook(new Thread(r));
	}
}

class TargetRunnable implements Runnable {
    
    @DirectCall(name = "callback", line = 22, resolvedTargets = "Ljvmc/Demo;")
    public void run(){
        Demo.callback();
    }
}
```
[//]: # (END)

##JVMC2
[//]: # (MAIN: jvmc.Demo)
This test case covers the ```finalize``` method which __might__ be called by the JVM during
garbage collection.
```java
// jvmc/Demo.java
package jvmc;


import lib.annotations.callgraph.DirectCall;

public class Demo {

    public static void callback(){};

	public static void main(String[] args){
          for(int i = -1; i < args.length; i++){
              new Demo();
          }
	}
	
	@DirectCall(name="callback", line=18, resolvedTargets = "Ljvmc/Demo;")
    public void finalize() throws java.lang.Throwable {
        callback();
        super.finalize();
    }	
}
```
[//]: # (END)

##JVMC3
[//]: # (MAIN: jvmc.Demo)
This cases tests the implicitly introduced call edge from ```Thread.start``` to ```Thread.run```.
Please note that this test tests this feature indirectly by validating that the run method of
```TargetRunnable``` is transitively reachable.
```java
// jvmc/Demo.java
package jvmc;

import lib.annotations.callgraph.IndirectCall;

public class Demo {

    @IndirectCall(name="run", line = 11, resolvedTargets = "Ljvmc/TargetRunnable;")
	public static void main(String[] args) throws InterruptedException {
        Runnable r = new TargetRunnable();
        Thread t = new Thread(r);
        t.start();
        t.join();
	}
}

class TargetRunnable implements Runnable {
    
    public void run(){
        /* Do the hard work */
    }   
}
```
[//]: # (END)

##JVMC4
[//]: # (MAIN: jvmc.Demo)
This cases tests the implicitly introduced call edge from ```Thread.start``` to the transitively
reachable ```Thread.exit``` method that is also called by the JVM on a thread's exit.
```java
// jvmc/Demo.java
package jvmc;

import lib.annotations.callgraph.IndirectCall;

public class Demo {

    @IndirectCall(name="exit", line = 12, resolvedTargets = "Ljava/lang/Thread;")
	public static void main(String[] args) throws InterruptedException {
        Runnable r = new TargetRunnable();
        Thread t = new Thread(r);
        t.start();
        t.join();
	}
}

class TargetRunnable implements Runnable {
    
    public void run(){
        /* Do the hard work */
    }   
}
```
[//]: # (END)

##JVMC5
[//]: # (MAIN: jvmc.Demo)
This cases tests the implicitly introduced call edge from ```Thread.setUncaughtExceptionHandler```
to ```Thread.dispatchUncaughtException``` method that is intended to be called by the JVM.
```java
// jvmc/Demo.java
package jvmc;

import lib.annotations.callgraph.DirectCall;

public class Demo {

	public static void main(String[] args) throws InterruptedException {
        Runnable r = new TargetRunnable();
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new ExceptionalExceptionHandler());
        t.start();
        t.join();
	}
}

class TargetRunnable implements Runnable {
    
    public void run(){
        throw new IllegalArgumentException("We don't want this thread to work!");
    }   
}

class ExceptionalExceptionHandler implements Thread.UncaughtExceptionHandler {
 
    private static void callback() { /* do something */ }
    
    @DirectCall(name="callback", line= 29, resolvedTargets = "Ljvmc/ExceptionalExceptionHandler;")
     public void uncaughtException(Thread t, Throwable e){
        callback();
        // Handle the uncaught Exception (IllegalArgumentException)
     }
}
```
[//]: # (END)