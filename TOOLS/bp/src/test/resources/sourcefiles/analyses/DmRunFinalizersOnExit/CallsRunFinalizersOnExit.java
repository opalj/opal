/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package DmRunFinalizersOnExit;

/**
 * Invokes System.runFinalizersOnExit, an extremely dangerous method.
 * 
 * @author Roberts Kolosovs
 */
public class CallsRunFinalizersOnExit {

    @SuppressWarnings("deprecation")
    public void doStuff() {
        System.runFinalizersOnExit(true);
    }
}
