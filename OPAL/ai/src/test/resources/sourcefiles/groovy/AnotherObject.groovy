/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package groovy;

import org.opalj.ai.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to constructors, instance methods and instance field accesses on another object.
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
public class AnotherObject {
    private SameObject o = new SameObject();
    
    @InvokedConstructor(receiverType=SameObject, lineNumber = 52)
    public void noArgumentsConstructor() {
        new SameObject();
    }

    @InvokedConstructor(receiverType=SameObject, parameterTypes=[int], lineNumber = 57)
    public void primitiveArgumentConstructor() {
        new SameObject(1);
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[Object], lineNumber = 62)
    public void objectArgumentConstructor() {
        new SameObject(new Object());
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[int[]], lineNumber = 67)
    public void primitiveVarargsConstructor() {
        new SameObject(1,2);
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[Object[]], lineNumber = 72)
    public void objectVarargsConstructor() {
        new SameObject(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="noParameters", parameterTypes=[], 
        lineNumber = 78, resolution = TargetResolution.DYNAMIC)
    public void runNoParameters() {
        o.noParameters();
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 84, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameter() {
        o.primitiveParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 90, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameter() {
        o.objectParameter(new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 96, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargs() {
        o.primitiveVarargs(1, 2);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 102, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargs() {
        o.objectVarargs(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 108, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameterBoxed() {
        o.primitiveParameter(Integer.valueOf(1));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 114, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameterUnboxed() {
        o.objectParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 120, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargsBoxed() {
        o.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 126, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargsUnboxed() {
        o.objectVarargs(1, 2);
    }
    
    @AccessedField(declaringType=SameObject, name="primitiveField", fieldType=int, lineNumber = 130)
    public int getPrimitiveField() { return o.primitiveField; }
    
    @AccessedField(declaringType=SameObject, fieldType=Object, name="objectField", lineNumber = 133)
    public Object getObjectField() { return o.objectField; }
    
    @InvokedMethod(receiverType=SameObject, name="noParameters", parameterTypes=[], 
        lineNumber = 138, resolution = TargetResolution.DYNAMIC)
    public void runNoParameters(SameObject o) {
        o.noParameters();
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 144, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameter(SameObject o) {
        o.primitiveParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 148, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameter(SameObject o) {
        o.objectParameter(new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 154, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargs(SameObject o) {
        o.primitiveVarargs(1, 2);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 162, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargs(SameObject o) {
        o.objectVarargs(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 168, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameterBoxed(SameObject o) {
        o.primitiveParameter(Integer.valueOf(1));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 174, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameterUnboxed(SameObject o) {
        o.objectParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 180, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargsBoxed(SameObject o) {
        o.primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 186, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargsUnboxed(SameObject o) {
        o.objectVarargs(1, 2);
    }
    
    @AccessedField(declaringType=SameObject, name="primitiveField", fieldType=int, lineNumber = 190)
    public int getPrimitiveField(SameObject o) { return o.primitiveField; }
    
    @AccessedField(declaringType=SameObject, fieldType=Object, name="objectField", lineNumber = 193)
    public Object getObjectField(SameObject o) { return o.objectField; }
}
