/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class CaughtException_mutation_3 {
    public static void main(String[] args) throws Exception {

        // 1. Try-catch where no error occurs
        try {
            int result = 5 + 5; // calculation stored in a temporary variable
            System.out.println("Test 1: No error occurs");
        } catch (Exception e) {
            System.out.println("This won't be printed since no exception occurs.");
        }

        // 2. Try-catch where an error occurs
        try {
            System.out.println("Test 2: An error will occur");
            int result = 10 / 0; // This causes an ArithmeticException
        } catch (Exception e) {
            System.out.println("Caught an exception in Test 2");
        }

        // 3. Try-catch-finally where no error occurs
        try {
            System.out.println("Test 3: No error, but with finally");
        } catch (Exception e) {
            System.out.println("This won't be printed since no exception occurs.");
        } finally {
            System.out.println("Test 3: Finally block executed");
        }

        // 4. Try-catch-finally with error from external method
        try {
            System.out.println("Test 4: An error will occur, with finally");
            exception();
        } catch (Exception e) {
            System.out.println("Caught an exception in Test 4");
        } finally {
            System.out.println("Test 4: Finally block executed");
        }

        // 5. Multiple catch blocks (wrong order test)
        try {
            System.out.println("Test 6: Multiple catch blocks with wrong order");
            int result = 10 / 0; // Causes ArithmeticException
        } catch (NullPointerException e) {
            System.out.println("Caught NullPointerException in Test 6 (should not be printed)");
        } catch (ArithmeticException e) {
            System.out.println("Caught ArithmeticException in Test 6 (correct)");
        } catch (Exception e) {
            System.out.println("Caught general Exception in Test 6 (should not be printed)");
        } finally {
            System.out.println("Test 6: Finally block executed");
        }

        // 6. inner try-catch + caused by method
        try {
            System.out.println("Test3 - outer try 1/6");
            try {
                System.out.println("Test3 - inner try 2/6");
                exception();
            } catch (Exception e) {
                System.out.println("Test3 - Caught Exception 3/6");
            } finally {
                System.out.println("Test3 - inner finally 4/6");
                exception();
            }
        } catch (Exception e) {
            System.out.println("Test3- Caught Exception 5/6");
        } finally {
            System.out.println("Test3 - Finally 6/6");
        }

    }

    public static void exception() throws Exception {
        int result = 10 / 0;
    }
}