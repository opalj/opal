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