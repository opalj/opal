/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ManualGarbageCollection;

/**
 * A class that invokes System.gc() in a method named gc(). The heuristic of the analysis
 * should ignore this case because it seems intentional.
 * 
 * @author Daniel Klauer
 */
public final class SystemGcIntentional {

    public static void gc() {
        java.lang.System.gc();
    }
}
