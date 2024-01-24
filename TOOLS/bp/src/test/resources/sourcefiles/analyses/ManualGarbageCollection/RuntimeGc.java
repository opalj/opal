/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ManualGarbageCollection;

/**
 * A class that invokes Runtime.gc() from the main() method. This should be reported.
 * 
 * @author Daniel Klauer
 */
public final class RuntimeGc {

    public static void main() {
        java.lang.Runtime.getRuntime().gc();
    }
}
