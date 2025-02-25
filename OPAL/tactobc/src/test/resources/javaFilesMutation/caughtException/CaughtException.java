public class CaughtException {

    static class CustomException extends Exception {
        public CustomException() {
            super("Custom Exception occurred!");
        }
    }

    public static void main(String[] args) {

        // should be caught
        catchException(new RuntimeException());
        catchException(new ArithmeticException());
        catchException(new CustomException());
        catchMultipleExceptions(new RuntimeException());
        catchMultipleExceptions(new ArithmeticException());
        catchMultipleExceptions(new CustomException());
        // should not be caught
        catchMultipleExceptions(new UnsupportedOperationException());
        try {
            throwException();
        } catch (Exception e) {
            System.out.println("Caught Exception from Method");
        }

    }

    public static void catchException(Exception exc) {
        try {
            throw exc;
        } catch (Exception e) {
            System.out.println("Caught Exception");
            e.printStackTrace();
        } finally {
            System.out.println("Finally block executed");
        }
    }

    public static void catchMultipleExceptions(Exception exc) {
        try {
            throw exc;
        } catch (RuntimeException e) {
            System.out.println("Caught RuntimeException");
            e.printStackTrace();
        } catch (ArithmeticException e) {
            System.out.println("Caught ArithmeticException");
            e.printStackTrace();
        } catch (CustomException e) {
            System.out.println("Caught CustomException");
            e.printStackTrace();
        } finally {
            System.out.println("Finally block executed");
        }
    }

    public static void throwException() throws Exception {
        throw new Exception("Method throws Exception");
    }
}