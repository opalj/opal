/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ManualGarbageCollection;

/**
 * A class that invokes System.gc() from the main() method. This should be reported.
 * 
 * @author Daniel Klauer
 */
public final class SystemGc {

    public static void main() {
        java.lang.System.gc();
    }
}
