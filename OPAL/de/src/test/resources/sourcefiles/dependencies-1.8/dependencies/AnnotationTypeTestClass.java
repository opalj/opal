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
package dependencies;

import java.io.FileReader;
import java.util.function.Function;
import java.util.function.Supplier;

import dependencies.OuterClass.InnerClass;

/**
 * @author Marco Jacobasch
 *
 */
@TypeTestAnnotation
@SuppressWarnings("unused")
public class AnnotationTypeTestClass {

    @TypeTestAnnotation
    int number = 0;

    @TypeTestAnnotation
    public void innerClass() {
        OuterClass.@TypeTestAnnotation InnerClass inner = null;
    }

    @TypeTestAnnotation
    @TypeTestAnnotation
    public void repeatableAnnotation() {
        @TypeTestAnnotation
        @TypeTestAnnotation
        int number = 0;
    }

    public void array() {
        @TypeTestAnnotation
        int @TypeTestAnnotation [] array = new @TypeTestAnnotation int @TypeTestAnnotation [] {};
    }

    public void twoDimArray() {
        @TypeTestAnnotation
        int @TypeTestAnnotation [][] array = new @TypeTestAnnotation int @TypeTestAnnotation [][] {};

        @TypeTestAnnotation
        int @TypeTestAnnotation [] @TypeTestAnnotation [] array2 = new @TypeTestAnnotation int @TypeTestAnnotation [] @TypeTestAnnotation [] {};
    }

    public void constructors() {
        String string = new @TypeTestAnnotation String();
        OuterClass outerClass = new OuterClass();
        InnerClass innerClass = outerClass.new @TypeTestAnnotation InnerClass(1);
    }

    public void cast() {
        TestInterface inter = (@TypeTestAnnotation TestInterface) new TestClass();
        inter.toString();
    }

    public void instance() {
        String string = new String();
        boolean check = string instanceof @TypeTestAnnotation String;
    }

    public void genericClass() {
        GenericTest<@TypeTestAnnotation Integer> generic = new GenericTest<>();
        generic = new GenericTest<@TypeTestAnnotation Integer>();
    }

    public void boundedTypes() {
        GenericTest<@TypeTestAnnotation ? super @TypeTestAnnotation String> instance = new GenericTest<>();
    }

    public void throwException() throws @TypeTestAnnotation IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    public void tryWithResource(String path) {
        try (@TypeTestAnnotation
        FileReader br = new @TypeTestAnnotation FileReader(path)) {
            // EMPTY
        } catch (Exception e) {
            // EMTPY
        }
    }

    public void tryCatch() {
        try {
            // EMPTY
        } catch (@TypeTestAnnotation Exception e) {
            // TODO: handle exception
        }
    }

    public void java8() {
        lambdaFunction("value", @TypeTestAnnotation String::toString);
        Supplier<TestClass> instance = @TypeTestAnnotation TestClass::new;
    }

    public static @TypeTestAnnotation String lambdaFunction(
            @TypeTestAnnotation String value,
            Function<@TypeTestAnnotation String, @TypeTestAnnotation String> function) {
        return function.apply(value);
    }

    /**
     * Inner class to annotate implements
     */
    public class Inheritance implements @TypeTestAnnotation TestInterface {

        @Override
        public void testMethod() {
            // EMPTY
        }

        @Override
        public String testMethod(Integer i, int j) {
            return null;
        }

    }

    /**
     * Inner class to annotate generic types
     */
    public class GenericTest<@TypeTestAnnotation T extends @TypeTestAnnotation Object> {
        
        public <@TypeTestAnnotation U> void inspect(U u){
            // EMPTY
        }

    }

    /**
     * Inner class to annotate generic intersection
     */
    public class GenericUpperIntersectionTest<@TypeTestAnnotation T extends @TypeTestAnnotation Object & @TypeTestAnnotation TypeTestAnnotation> {

    }

    /**
     * Inner class to test nested generics
     */
    public @TypeTestAnnotation class NestedGeneric<@TypeTestAnnotation T extends @TypeTestAnnotation GenericTest<@TypeTestAnnotation U>, @TypeTestAnnotation U> {

    }

}
