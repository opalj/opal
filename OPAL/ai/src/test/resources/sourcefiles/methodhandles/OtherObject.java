/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * -->
 *
 * @author Arne Lottmann
 */
public class OtherObject {
	private final Lookup lookup = MethodHandles.lookup();
	
	private final SameObject thisArgument = new SameObject();
	
	public void noArgumentsPublic() {/*empty*/}
	
	protected void noArgumentsProtected() {/*empty*/}
	
	@SuppressWarnings("all")
	private void noArgumentsPrivate() {/*empty*/}
	
	@InvokedMethods({
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "noArgumentsPublic", isReflective = true, line = 71),
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "noArgumentsProtected", isReflective = true, line = 73),
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "noArgumentsPrivate", isReflective = true, line = 75)
	})
	public void noArgumentsMethodHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class);
		MethodHandle publicHandle = lookup.findVirtual(SameObject.class, "noArgumentsPublic", voidType);
		publicHandle.invokeExact(thisArgument);
		MethodHandle protectedHandle = lookup.findVirtual(SameObject.class, "noArgumentsProtected", voidType);
		protectedHandle.invokeExact(thisArgument);
		MethodHandle privateHandle = lookup.findVirtual(SameObject.class, "noArgumentsPrivate", voidType);
		privateHandle.invokeExact(thisArgument);
	}
	
	public void primitive(int i) {/*empty*/}
	
	public int primitiveReturn(int i) { return i; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "primitive", parameterTypes = { int.class }, isReflective = true, line = 89),
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "primitiveReturn", parameterTypes = { int.class }, returnType = int.class, isReflective = true, line = 92)
	})
	public void primitiveHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, int.class);
		MethodHandle voidHandle = lookup.findVirtual(SameObject.class, "primitive", voidType);
		voidHandle.invokeExact(thisArgument, 1);
		MethodType returnType = MethodType.methodType(int.class, int.class);
		MethodHandle returnHandle = lookup.findVirtual(SameObject.class, "primitiveReturn", returnType);
		returnHandle.invokeExact(thisArgument, 1);
	}
	
	public void object(Object o) {/*empty*/}
	
	public Object objectReturn(Object o) { return o; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "object", parameterTypes = { Object.class }, isReflective = true, line = 106),
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "objectReturn", parameterTypes = { Object.class }, returnType = Object.class, isReflective = true, line = 109)
	})
	public void objectHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, Object.class);
		MethodHandle voidHandle = lookup.findVirtual(SameObject.class, "object", voidType);
		voidHandle.invokeExact(thisArgument, new Object());
		MethodType returnType = MethodType.methodType(Object.class, Object.class);
		MethodHandle returnHandle = lookup.findVirtual(SameObject.class, "objectReturn", returnType);
		returnHandle.invokeExact(thisArgument, new Object());
	}
	
	public void array(int[] array) {/*empty*/}
	
	public int[] arrayReturn(int[] array) { return array; }
	
	@InvokedMethods({
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "array", parameterTypes = { int[].class }, isReflective = true, line = 123),
		@InvokedMethod(receiverType = "methodhandles/SameObject", name = "arrayReturn", parameterTypes = { int[].class }, returnType = int[].class, isReflective = true, line = 126)
	})
	public void arrayHandles() throws Throwable {
		MethodType voidType = MethodType.methodType(Void.class, int[].class);
		MethodHandle voidHandle = lookup.findVirtual(SameObject.class, "array", voidType);
		voidHandle.invokeExact(thisArgument, new int[] { 1 });
		MethodType returnType = MethodType.methodType(int[].class, int[].class);
		MethodHandle returnHandle = lookup.findVirtual(SameObject.class, "arrayReturn", returnType);
		returnHandle.invokeExact(thisArgument, new int[] { 1 });
	}
}
