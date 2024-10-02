/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.io.FileReader;
import java.util.function.Function;
import java.util.function.Supplier;

import dependencies.OuterClass.InnerClass;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Marco Jacobasch
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
