/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * A class that contains many examples related to the exception handling.
 *
 * @author Michael Eichberg
 */
public class ExceptionValuesFrenzy {

    final private RuntimeException e;

    public ExceptionValuesFrenzy(RuntimeException e) {
        this.e = e;
    }

    static void throwThrowable() throws Throwable {
        throw new Throwable();
    }

    void throwException() {
        throw e;
    }

    // The allocation site of all exceptions (in this case) should be external to the method but
    // related to the method call.
    static void handleExceptions() throws Throwable {
        try {
            throwThrowable();
        } catch (RuntimeException e) {
            System.out.println("Exception handled!");
            throw e;
        }
    }

    // The allocation site of all exceptions (in this case) should be either external to the method
    // but related to the method call or be the call itself if the receiver is null.
    static void handleExceptions(ExceptionValuesFrenzy receiver) {
        try {
            receiver.throwException();
        } catch (RuntimeException e) {
            System.out.println("Exception handled!");
            throw e;
        }
    }

    // The allocation site of all exceptions (in this case) should be the VM as a side effect of the
    // handle exceptions method. However, the exception type "ArrayStoreException" should not occur
    // among the set of thrown exceptions, because it is explicitly handled and not rethrown.
    @SuppressWarnings("all")
    static <T> void handleExceptions(T[] a, Object o) {
        try {
            a[0] = (T) o;
        } catch (ArrayStoreException e) {
            System.out.println("Exception handled!");
        }
    }

}
