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
package groovy;

import org.opal.ai.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to static methods and static field accesses within the same class.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * 
 * 
 * -->
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
    
    @InvokedMethod(receiverType=SameClass, name="noParameters", parameterTypes=[], 
        lineNumber = 64, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runNoParameters() {
        noParameters();
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 70, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveParameter() {
        primitiveParameter(1);
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 76, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectParameter() {
        objectParameter(new Object());
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 82, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveVarargs() {
        primitiveVarargs(1, 2);
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 88, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectVarargs() {
        objectVarargs(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 94, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveParameterBoxed() {
        primitiveParameter(Integer.valueOf(1));
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 100, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectParameterUnboxed() {
        objectParameter(1);
    }
    
    @InvokedMethod(receiverType=SameClass, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 106, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runPrimitiveVarargsBoxed() {
        primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
    }
    
    @InvokedMethod(receiverType=SameClass, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 112, isStatic = true, resolution = TargetResolution.DYNAMIC)
    public static void runObjectVarargsUnboxed() {
        objectVarargs(1, 2);
    }
    
    @AccessedField(declaringType=SameClass, name="primitiveField", fieldType=int, lineNumber = 117,
        isStatic = true)
    public static int getPrimitiveField() { return primitiveField; }
    
    @AccessedField(declaringType=SameClass, fieldType=Object, name="objectField", lineNumber = 121,
        isStatic = true)
    public static Object getObjectField() { return objectField; }
}
