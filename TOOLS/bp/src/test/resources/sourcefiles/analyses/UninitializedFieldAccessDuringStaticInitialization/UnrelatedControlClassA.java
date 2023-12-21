/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package UninitializedFieldAccessDuringStaticInitialization;

/**
 * Invokes System.runFinalizersOnExit, an extremely dangerous method.
 * 
 * @author Roberts Kolosovs
 */
public class UnrelatedControlClassA {

    @SuppressWarnings("deprecation")
    public void doStuff() {
        System.runFinalizersOnExit(true);
    }

    static int bla = UnrelatedControlClassB.blubb;
}
