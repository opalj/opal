/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class InvokeDynamic {

    public static void main(String[] args) {
        // Lambda Expression
        Runnable r = () -> System.out.println("Running in a lambda!");
        r.run();

        // String Concatenation
        System.out.println("Hello, World!");

        // Method Reference
        new InvokeDynamic().methodReferenceExample();
    }

    public void methodReferenceExample() {
        System.out.println("Method Reference Example");
    }
}
