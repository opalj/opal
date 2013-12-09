package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to constructors, instance methods and instance field accesses on another object.
 * 
 * @author Arne Lottmann
 */
public class AnotherObject {
	private SameObject o = new SameObject();
	
	@InvokeDynamicConstructor(receiverType=SameObject)
	public void noArgumentsConstructor() {
		new SameObject();
	}

	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[int])
	public void primitiveArgumentConstructor() {
		new SameObject(1);
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[Object])
	public void objectArgumentConstructor() {
		new SameObject(new Object());
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[int[]])
	public void primitiveVarargsConstructor() {
		new SameObject(1,2);
	}
	
	@InvokeDynamicConstructor(receiverType=SameObject, parameterTypes=[Object[]])
	public void objectVarargsConstructor() {
		new SameObject(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="noParameters", returnType=Void,
		parameterTypes=[])
	public void runNoParameters() {
		o.noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameter() {
		o.primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameter() {
		o.objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargs() {
		o.primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargs() {
		o.objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameterBoxed() {
		o.primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameterUnboxed() {
		o.objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargsBoxed() {
		o.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargsUnboxed() {
		o.objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=SameObject, name="primitiveField", fieldType=int)
	public int getPrimitiveField() { return o.primitiveField; }
	
	@InvokeDynamicField(declaringType=SameObject, fieldType=Object, name="objectField")
	public Object getObjectField() { return o.objectField; }
	
	@InvokeDynamicMethod(receiverType=SameObject, name="noParameters", returnType=Void,
		parameterTypes=[])
	public void runNoParameters(SameObject o) {
		o.noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameter(SameObject o) {
		o.primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameter(SameObject o) {
		o.objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargs(SameObject o) {
		o.primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargs(SameObject o) {
		o.objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameterBoxed(SameObject o) {
		o.primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameterUnboxed(SameObject o) {
		o.objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargsBoxed(SameObject o) {
		o.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=SameObject, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargsUnboxed(SameObject o) {
		o.objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=SameObject, name="primitiveField", fieldType=int)
	public int getPrimitiveField(SameObject o) { return o.primitiveField; }
	
	@InvokeDynamicField(declaringType=SameObject, fieldType=Object, name="objectField")
	public Object getObjectField(SameObject o) { return o.objectField; }
}
