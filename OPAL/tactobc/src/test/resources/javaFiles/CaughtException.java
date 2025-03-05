public class CaughtException {
    public static void main(String[] args) {

        // 1. Try-catch where no error occurs
        try {
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

        // 4. Try-catch-finally where an error occurs
        try {
            System.out.println("Test 4: An error will occur, with finally");
            int result = 10 / 0;
        } catch (Exception e) {
            System.out.println("Caught an exception in Test 4");
        } finally {
            System.out.println("Test 4: Finally block executed");
        }

        // 5. Try-finally without catch (finally always runs)
        try {
            System.out.println("Test 5: Try-finally without catch");
        } finally {
            System.out.println("Test 5: Finally block executed");
        }

        // 6. Multiple catch blocks (wrong order test)
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
    }
}
