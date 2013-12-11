package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to static methods and static field accesses to
 * another class.
 * 
 * @author Arne Lottmann
 */
public class AnotherClass {
	@InvokeDynamicMethod(receiverType=SameClass, name="noParameters", returnType=Void,
		parameterTypes=[])
	public static void runNoParameters() {
		SameClass.noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public static void runPrimitiveParameter() {
		SameClass.primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public static void runObjectParameter() {
		SameClass.objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public static void runPrimitiveVarargs() {
		SameClass.primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public static void runObjectVarargs() {
		SameClass.objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public static void runPrimitiveParameterBoxed() {
		SameClass.primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public static void runObjectParameterUnboxed() {
		SameClass.objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public static void runPrimitiveVarargsBoxed() {
		SameClass.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=SameClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public static void runObjectVarargsUnboxed() {
		SameClass.objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=SameClass, name="primitiveField", fieldType=int)
	public static int getPrimitiveField() { return SameClass.primitiveField; }
	
	@InvokeDynamicField(declaringType=SameClass, fieldType=Object, name="objectField")
	public static Object getObjectField() { return SameClass.objectField; }
}
