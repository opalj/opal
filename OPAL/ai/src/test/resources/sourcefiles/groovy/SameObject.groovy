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

import org.opalj.ai.test.invokedynamic.annotations.*;

/**
 * Test class for resolving invokedynamic calls to constructor calls, instance methods and instance field accesses within the same object.
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
public class SameObject {
    protected int primitiveField = 1;
    protected Object objectField = new Object();
    
    public SameObject() {}
    public SameObject(int primitive) {}
    public SameObject(Object object) {}
    public SameObject(int...varargs) {}
    public SameObject(Object...varargs) {}
    
    @InvokedConstructor(receiverType=SameObject, lineNumber = 59)
    public static void noArgumentsConstructor() {
        new SameObject();
    }

    @InvokedConstructor(receiverType=SameObject, parameterTypes=[int], lineNumber = 64)
    public static void primitiveArgumentConstructor() {
        new SameObject(1);
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[Object], lineNumber = 69)
    public static void objectArgumentConstructor() {
        new SameObject(new Object());
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[int[]], lineNumber = 74)
    public static void primitiveVarargsConstructor() {
        new SameObject(1,2);
    }
    
    @InvokedConstructor(receiverType=SameObject, parameterTypes=[Object[]], lineNumber = 79)
    public static void objectVarargsConstructor() {
        new SameObject(new Object(), new Object());
    }
    
    public void noParameters() {}
    
    public void primitiveParameter(int primitive) {}
    
    public void objectParameter(Object object) {}
    
    public void primitiveVarargs(int...varargs) {}
    
    public void objectVarargs(Object...varargs) {}
    
    @InvokedMethod(receiverType=SameObject, name="noParameters", parameterTypes=[], 
        lineNumber = 95, resolution = TargetResolution.DYNAMIC)
    public void runNoParameters() {
        noParameters();
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 101, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameter() {
        primitiveParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 107, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameter() {
        objectParameter(new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 113, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargs() {
        primitiveVarargs(1, 2);
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 119, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargs() {
        objectVarargs(new Object(), new Object());
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveParameter", parameterTypes=[int], 
        lineNumber = 125, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveParameterBoxed() {
        primitiveParameter(Integer.valueOf(1));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectParameter", parameterTypes=[Object], 
        lineNumber = 131, resolution = TargetResolution.DYNAMIC)
    public void runObjectParameterUnboxed() {
        objectParameter(1);
    }
    
    @InvokedMethod(receiverType=SameObject, name="primitiveVarargs", parameterTypes=[int[]], 
        lineNumber = 137, resolution = TargetResolution.DYNAMIC)
    public void runPrimitiveVarargsBoxed() {
        primitiveVarargs(Integer.valueOf(1), Integer.valueOf(2));
    }
    
    @InvokedMethod(receiverType=SameObject, name="objectVarargs", parameterTypes=[Object[]], 
        lineNumber = 143, resolution = TargetResolution.DYNAMIC)
    public void runObjectVarargsUnboxed() {
        objectVarargs(1, 2);
    }
    
    @AccessedField(declaringType=SameObject, name="primitiveField", fieldType=int, lineNumber = 147)
    public int getPrimitiveField() { return primitiveField; }
    
    @AccessedField(declaringType=SameObject, fieldType=Object, name="objectField", lineNumber = 150)
    public Object getObjectField() { return objectField; }
}
