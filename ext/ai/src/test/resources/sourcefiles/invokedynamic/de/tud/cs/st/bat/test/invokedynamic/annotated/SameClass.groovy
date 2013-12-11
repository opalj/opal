package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to static methods and static field accesses within the same class.
 * 
 * @author Arne Lottmann
 */
public class SameClass {
	protected static int primitiveField = 1;
	protected static Object objectField = new Object();
	
	public static void noParameters() {}
	
	public static void primitiveParameter(int primitive) {}
	
	public static void objectParameter(Object object) {}
	
	public static void primitiveVarargs(int...varargs) {}
	
	public static void objectVarargs(Object...varargs) {}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="noParameters", returnType=Void,
		parameterTypes=[])
	public static void runNoParameters() {
		noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public static void runPrimitiveParameter() {
		primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public static void runObjectParameter() {
		objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public static void runPrimitiveVarargs() {
		primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public static void runObjectVarargs() {
		objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public static void runPrimitiveParameterBoxed() {
		primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public static void runObjectParameterUnboxed() {
		objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public static void runPrimitiveVarargsBoxed() {
		primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public static void runObjectVarargsUnboxed() {
		objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=SameClass, name="primitiveField", fieldType=int)
	public static int getPrimitiveField() { return primitiveField; }
	
	@InvokeDynamicField(declaringType=SameClass, fieldType=Object, name="objectField")
	public static Object getObjectField() { return objectField; }
}
