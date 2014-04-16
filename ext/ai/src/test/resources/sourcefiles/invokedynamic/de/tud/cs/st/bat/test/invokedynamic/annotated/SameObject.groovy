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
package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to constructor calls, instance methods and instance field accesses within the same object.
 * 
 * @author Arne Lottmann
 */
public class SameObject {
	protected int primitiveField = 1;
	protected Object objectField = new Object();
	
	public SameObject() {}
	public SameObject(int primitive) {}
	public SameObject(Object object) {}
	public SameObject(int...varargs) {}
	public SameObject(Object...varargs) {}
	
	@InvokeDynamicConstructor(receiverType=SameObject)
	public static void noArgumentsConstructor() {
		new SameObject();
	}

	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[int])
	public static void primitiveArgumentConstructor() {
		new SameObject(1);
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[Object])
	public static void objectArgumentConstructor() {
		new SameObject(new Object());
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[int[]])
	public static void primitiveVarargsConstructor() {
		new SameObject(1,2);
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[Object[]])
	public static void objectVarargsConstructor() {
		new SameObject(new Object(), new Object());
	}
	
	public void noParameters() {}
	
	public void primitiveParameter(int primitive) {}
	
	public void objectParameter(Object object) {}
	
	public void primitiveVarargs(int...varargs) {}
	
	public void objectVarargs(Object...varargs) {}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="noParameters", returnType=Void,
		parameterTypes=[])
	public void runNoParameters() {
		noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameter() {
		primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameter() {
		objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargs() {
		primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargs() {
		objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameterBoxed() {
		primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameterUnboxed() {
		objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargsBoxed() {
		primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargsUnboxed() {
		objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=SameObject, name="primitiveField", fieldType=int)
	public int getPrimitiveField() { return primitiveField; }
	
	@InvokeDynamicField(declaringType=SameObject, fieldType=Object, name="objectField")
	public Object getObjectField() { return objectField; }
}