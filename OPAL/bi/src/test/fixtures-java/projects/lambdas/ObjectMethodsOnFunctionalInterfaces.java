/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
