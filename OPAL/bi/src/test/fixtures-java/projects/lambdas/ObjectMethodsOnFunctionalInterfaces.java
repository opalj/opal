/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.*;


/**
 * Test cases to show that calls to inherited methods on lambda instances go to Object.
 *
 * 
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 *
 * @author Arne Lottmann
 */
public class ObjectMethodsOnFunctionalInterfaces {
  
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "equals", parameterTypes = { Object.class }, returnType = boolean.class, isStatic = false, line = 54)
	public void lambdaEquals() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.equals(new Object());
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "getClass", returnType = Class.class, isStatic = false, line = 60)
	public void lambdaGetClass() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.getClass();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "hashCode", returnType = int.class, isStatic = false, line = 66)
	public void lambdaHashCode() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.hashCode();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "notify", isStatic = false, line = 72)
	public void lambdaNotify() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.notify();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "notifyAll", isStatic = false, line = 78)
	public void lambdaNotifyAll() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.notifyAll();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "toString", returnType = String.class, isStatic = false, line = 84)
	public void lambdaToString() {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.toString();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "wait", isStatic = false, line = 90)
	public void lambdaWait() throws InterruptedException {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.wait();
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "wait", parameterTypes = { long.class }, isStatic = false, line = 96)
	public void lambdaWaitLong() throws InterruptedException {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.wait(1);
	}
	
	@InvokedMethod(resolution = DEFAULT, receiverType = "java/lang/Object", name = "wait", parameterTypes = { long.class, int.class }, isStatic = false, line = 102)
	public void lambdaWaitLongInt() throws InterruptedException {
		Runnable lambda = () -> System.out.println("Hello world!");
		lambda.wait(1, 1);
	}
}
