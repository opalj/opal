package de.tud.cs.st.bat.test.invokedynamic.annotated;

import de.tud.cs.st.bat.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to methods set up using groovy's metaclass.
 * 
 * I'm not yet sure what class the receiverType / declaringType should be, either MyClass or whatever its metaclass is.
 * I find using MyClass more reasonable though, since that is the type we're invoking methods on.
 * 
 * @author Arne Lottmann
 *
 */
public class WithMetaClass {
	private class MyClass {
		
	}

	private MyClass myObject;
		
	public WithMetaClass() {
		myObject = new MyClass();
		setupMethods(myObject);
		setupFields(myObject);
	}
	
	private void setupMethods(MyClass obj) {
		obj.metaClass.constructor << { int primitive -> }
		obj.metaClass.constructor << { Object object -> }
		obj.metaClass.constructor << { int...varargs -> }
		obj.metaClass.constructor << { Object...varargs -> }
		
		obj.metaClass.noParameters = { -> }
		obj.metaClass.primitiveParameter = { int primitive -> }
		obj.metaClass.objectParameter = { Object object -> }
		obj.metaClass.primitiveVarargs = { int...varargs -> }
		obj.metaClass.objectVarargs = { Object...varargs -> }
	}
	
	private void setupFields(MyClass obj) {
		obj.metaClass.primitiveField = 1;
		obj.metaClass.objectField = new Object();
	}
	
	@InvokeDynamicConstructor(receiverType=MyClass)
	public void noArgumentsConstructor() {
		new MyClass();
	}
	
	@InvokeDynamicConstructor(receiverType=MyClass, parameterTypes=[int])
	public void primitiveArgumentConstructor() {
		new MyClass(1);
	}
	
	@InvokeDynamicConstructor(receiverType=MyClass, parameterTypes=[Object])
	public void objectArgumentConstructor() {
		new MyClass(new Object());
	}
	
	@InvokeDynamicConstructor(receiverType=MyClass, parameterTypes=[int[]])
	public void primitiveVarargsConstructor() {
		new MyClass(1,2);
	}
	
	@InvokeDynamicConstructor(receiverType=MyClass, parameterTypes=[Object[]])
	public void objectVarargsConstructor() {
		new MyClass(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="noParameters", returnType=Void,
		parameterTypes=[])
	public void runNoParameters() {
		myObject.noParameters();
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameter() {
		myObject.primitiveParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameter() {
		myObject.objectParameter(new Object());
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargs() {
		myObject.primitiveVarargs(1, 2);
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargs() {
		myObject.objectVarargs(new Object(), new Object());
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="primitiveParameter", returnType=Void,
		parameterTypes=[int])
	public void runPrimitiveParameterBoxed() {
		myObject.primitiveParameter(Integer.valueOf(1));
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="objectParameter", returnType=Void,
		parameterTypes=[Object])
	public void runObjectParameterUnboxed() {
		myObject.objectParameter(1);
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="primitiveVarargs", returnType=Void,
		parameterTypes=[int[]])
	public void runPrimitiveVarargsBoxed() {
		myObject.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
	}
	
	@InvokeDynamicMethod(receiverType=MyClass, name="objectVarargs", returnType=Void,
		parameterTypes=[Object[]])
	public void runObjectVarargsUnboxed() {
		myObject.objectVarargs(1, 2);
	}
	
	@InvokeDynamicField(declaringType=MyClass, name="primitiveField", fieldType=int)
	public int getPrimitiveField() { return primitiveField; }
	
	@InvokeDynamicField(declaringType=MyClass, fieldType=Object, name="objectField")
	public Object getObjectField() { return objectField; }
}