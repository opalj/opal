/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class InstanceOf_mutation_2 {

    public static void main(String[] args) {
        // Create a class with static fields
        class MyClass {
            static Object obj = "Hello, World!";
            static String str = "Hello, World!";
            static Integer integer = new Integer(5);
        }
        // Test with String
        System.out.println("obj is an instance of String: " + (MyClass.obj instanceof String)); // true
        System.out.println("obj is an instance of CharSequence: " + (MyClass.obj instanceof CharSequence)); // true
        System.out.println("obj is an instance of Object: " + (MyClass.obj instanceof Object)); // true
        System.out.println("obj is an instance of Integer: " + (MyClass.obj instanceof Integer)); // false
    }
}