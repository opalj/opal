/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package methodhandles;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;

import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

/**
 * Test cases for method handles.
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * 
 * 
 * 
 * -->
 *
 * @author Arne Lottmann
 */
public class SameClass {
	private static final Lookup lookup = MethodHandles.lookup();
	
	public static void noArgumentsPublic() {}
	
	protected static void noArgumentsProtected() {}
	
	private static void noArgumentsPrivate() {}
	
	@InvokedMethods({
		@InvokedMethod(receiverType = SameClass.class, name = "noArgumentsPublic", isStatic = true, isReflective = true, lineNumber = 71),
		@InvokedMethod(receiverType = SameClass.class, name = "noArgumentsProtected", isStatic = true, isReflective = true, lineNumber = 73),
		@InvokedMethod(receiverType = SameClass.class, name = "noArgumentsPrivate", isStatic = true, isReflective = true, lineNumber = 75)
	})
	public void noArgumentsMethodHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class);
		MethodHandle publicHandle = lookup.findStatic(SameClass.class, "noArgumentsPublic", voidType);
		publicHandle.invokeExact();
		MethodHandle protectedHandle = lookup.findStatic(SameClass.class, "noArgumentsProtected", voidType);
		protectedHandle.invokeExact();
		MethodHandle privateHandle = lookup.findStatic(SameClass.class, "noArgumentsPrivate", voidType);
		privateHandle.invokeExact();
	}
	
	public static void primitive(int i) {}
	
	public static int primitiveReturn(int i) { return i; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = SameClass.class, name = "primitive", parameterTypes = { int.class }, isStatic = true, isReflective = true, lineNumber = 89),
		@InvokedMethod(receiverType = SameClass.class, name = "primitiveReturn", parameterTypes = { int.class }, returnType = int.class, isStatic = true, isReflective = true, lineNumber = 92)
	})
	public static void primitiveHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, int.class);
		MethodHandle voidHandle = lookup.findStatic(SameClass.class, "primitive", voidType);
		voidHandle.invokeExact(1);
		MethodType returnType = MethodType.methodType(int.class, int.class);
		MethodHandle returnHandle = lookup.findStatic(SameClass.class, "primitiveReturn", returnType);
		returnHandle.invokeExact(1);
	}
	
	public static void object(Object o) {}
	
	public static Object objectReturn(Object o) { return o; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = SameClass.class, name = "object", parameterTypes = { Object.class }, isStatic = true, isReflective = true, lineNumber = 106),
		@InvokedMethod(receiverType = SameClass.class, name = "objectReturn", parameterTypes = { Object.class }, returnType = Object.class, isStatic = true, isReflective = true, lineNumber = 109)
	})
	public static void objectHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, Object.class);
		MethodHandle voidHandle = lookup.findStatic(SameClass.class, "object", voidType);
		voidHandle.invokeExact(new Object());
		MethodType returnType = MethodType.methodType(Object.class, Object.class);
		MethodHandle returnHandle = lookup.findStatic(SameClass.class, "objectReturn", returnType);
		returnHandle.invokeExact(new Object());
	}
	
	public static void array(int[] array) {}
	
	public static int[] arrayReturn(int[] array) { return array; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = SameClass.class, name = "array", parameterTypes = { int[].class }, isStatic = true, isReflective = true, lineNumber = 123),
		@InvokedMethod(receiverType = SameClass.class, name = "arrayReturn", parameterTypes = { int[].class }, returnType = int[].class, isStatic = true, isReflective = true, lineNumber = 126)
	})
	public static void arrayHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, int[].class);
		MethodHandle voidHandle = lookup.findStatic(SameClass.class, "array", voidType);
		voidHandle.invokeExact(new int[] { 1 });
		MethodType returnType = MethodType.methodType(int[].class, int[].class);
		MethodHandle returnHandle = lookup.findStatic(SameClass.class, "arrayReturn", returnType);
		returnHandle.invokeExact(new int[] { 1 });
	}
}
