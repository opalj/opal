/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class ThrowException {
    public static void main(String[] args) {

        // 1. new Exception
        try {
            System.out.println("Test1 - Try");
            throw new Exception("Test1 - Exception");
        } catch (Exception e) {
            System.out.println("Test1 - Caught Exception");
        }

        // 2. same Exception object twice
        Exception exc = new Exception("Test2 - Exception");
        try {
            System.out.println("Test2 - Try");
            throw exc;
        } catch (Exception e) {
            System.out.println("Test2 - Caught Exception");
        }
        try {
            System.out.println("Test2 - Try2");
            throw exc;
        } catch (Exception e) {
            System.out.println("Test2 - Caught Exception2");
        }

        // 3. new Exception + finally
        try {
            System.out.println("Test3 - Try");
            throw new Exception("Test3 - Exception");
        } catch (Exception e) {
            System.out.println("Test3 - Caught Exception");
        } finally {
            System.out.println("Test3 - Finally");
        }

        // 4. new Exception + multiple catch blocks
        try {
            System.out.println("Test4 - Try");
            throw new ArithmeticException("Test4 - ArithmeticException");
        } catch (NullPointerException e) {
            System.out.println("Test4 - NullPointerException *SHOULD NOT EXECUTE*");
        } catch (ArithmeticException e) {
            System.out.println("Test4 - ArithmeticException");
        } catch (Exception e) {
            System.out.println("Test4 - Caught Exception *SHOULD NOT EXECUTE*");
        } finally {
            System.out.println("Test4 - Finally");
        }

        // 5. method external exception with throw
        try {
            System.out.println("Test5 - Try");
            exception();
        } catch (Exception e) {
            System.out.println("Test5 - Caught Exception");
        }
    }

    public static void exception() throws Exception {
        throw new Exception();
    }
}