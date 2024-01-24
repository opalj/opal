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
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * 
 * @author Arne Lottmann
 */
public class SameClass {

    private static final Lookup lookup = MethodHandles.lookup();

    public static void noArgumentsPublic() {/* empty */ }

    protected static void noArgumentsProtected() {/* empty */ }

    @SuppressWarnings("all")
    private static void noArgumentsPrivate() {/* empty */ }

    @InvokedMethods({
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "noArgumentsPublic", isStatic = true, isReflective = true, line = 71),
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "noArgumentsProtected", isStatic = true, isReflective = true, line = 73),
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "noArgumentsPrivate", isStatic = true, isReflective = true, line = 75) })
    public void noArgumentsMethodHandles() throws Throwable {
        MethodType voidType = MethodType.methodType(Void.class);
        MethodHandle publicHandle = lookup.findStatic(SameClass.class, "noArgumentsPublic", voidType);
        publicHandle.invokeExact();
        MethodHandle protectedHandle = lookup.findStatic(SameClass.class, "noArgumentsProtected", voidType);
        protectedHandle.invokeExact();
        MethodHandle privateHandle = lookup.findStatic(SameClass.class, "noArgumentsPrivate", voidType);
        privateHandle.invokeExact();
    }
    

    public static void primitive(int i) { /* empty */ }

    public static int primitiveReturn(int i) { return i; }

    @InvokedMethods({
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "primitive", parameterTypes = { int.class }, isStatic = true, isReflective = true, line = 89),
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "primitiveReturn", parameterTypes = { int.class }, returnType = int.class, isStatic = true, isReflective = true, line = 92) })
    public static void primitiveHandles() throws Throwable {
        MethodType voidType = MethodType.methodType(Void.class, int.class);
        MethodHandle voidHandle = lookup.findStatic(SameClass.class, "primitive", voidType);
        voidHandle.invokeExact(1);
        MethodType returnType = MethodType.methodType(int.class, int.class);
        MethodHandle returnHandle = lookup.findStatic(SameClass.class, "primitiveReturn", returnType);
        returnHandle.invokeExact(1);
    }

    
    public static void object(Object o) { /* empty */ }

    public static Object objectReturn(Object o) { return o; }

    @InvokedMethods({
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "object", parameterTypes = { Object.class }, isStatic = true, isReflective = true, line = 106),
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "objectReturn", parameterTypes = { Object.class }, returnType = Object.class, isStatic = true, isReflective = true, line = 109) })
    public static void objectHandles() throws Throwable {
        MethodType voidType = MethodType.methodType(Void.class, Object.class);
        MethodHandle voidHandle = lookup.findStatic(SameClass.class, "object", voidType);
        voidHandle.invokeExact(new Object());
        MethodType returnType = MethodType.methodType(Object.class, Object.class);
        MethodHandle returnHandle = lookup.findStatic(SameClass.class, "objectReturn", returnType);
        returnHandle.invokeExact(new Object());
    }

    
    public static void array(int[] array) {/* empty */ }

    public static int[] arrayReturn(int[] array) { return array; }

    @InvokedMethods({
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "array", parameterTypes = { int[].class }, isStatic = true, isReflective = true, line = 123),
            @InvokedMethod(receiverType = "methodhandles/SameClass", name = "arrayReturn", parameterTypes = { int[].class }, returnType = int[].class, isStatic = true, isReflective = true, line = 126) })
    public static void arrayHandles() throws Throwable {
        MethodType voidType = MethodType.methodType(Void.class, int[].class);
        MethodHandle voidHandle = lookup.findStatic(SameClass.class, "array", voidType);
        voidHandle.invokeExact(new int[] { 1 });
        MethodType returnType = MethodType.methodType(int[].class, int[].class);
        MethodHandle returnHandle = lookup.findStatic(SameClass.class, "arrayReturn", returnType);
        returnHandle.invokeExact(new int[] { 1 });
    }
}
