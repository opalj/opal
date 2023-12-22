/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package groovy;

import org.opalj.ai.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to static methods and static field accesses to
 * another class.
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
public class AnotherClass {
    @InvokedMethod(receiverType=SameClass, name="noParameters", parameterTypes=[],
        lineNumber = 52, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runNoParameters() {
        SameClass.noParameters();
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveParameter", parameterTypes=[int],
        lineNumber = 58, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveParameter() {
        SameClass.primitiveParameter(1);
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectParameter", parameterTypes=[Object],
        lineNumber = 64, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectParameter() {
        SameClass.objectParameter(new Object());
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveVarargs", parameterTypes=[int[]],
        lineNumber = 70, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveVarargs() {
        SameClass.primitiveVarargs(1, 2);
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectVarargs", parameterTypes=[Object[]],
        lineNumber = 76, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectVarargs() {
        SameClass.objectVarargs(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveParameter", parameterTypes=[int],
        lineNumber = 82, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveParameterBoxed() {
        SameClass.primitiveParameter(Integer.valueOf(1));
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectParameter", parameterTypes=[Object],
        lineNumber = 88, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectParameterUnboxed() {
        SameClass.objectParameter(1);
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveVarargs", parameterTypes=[int[]],
        lineNumber = 94, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveVarargsBoxed() {
        SameClass.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectVarargs", parameterTypes=[Object[]],
        lineNumber = 100, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectVarargsUnboxed() {
        SameClass.objectVarargs(1, 2);
    }
    
    @AccessedField(declaringType=SameClass, name="primitiveField", fieldType=int, isStatic = true,
        lineNumber = 105)
    public static int getPrimitiveField() { return SameClass.primitiveField; }
    
    @AccessedField(declaringType=SameClass, fieldType=Object, name="objectField", isStatic = true,
        lineNumber = 109)
    public static Object getObjectField() { return SameClass.objectField; }
}
