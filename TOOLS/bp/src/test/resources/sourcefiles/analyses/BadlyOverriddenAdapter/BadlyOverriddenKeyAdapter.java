/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `KeyAdapter` but does not override its `keyTyped()` properly,
 * because the signatures of the `keyTyped()` here is incompatible to that of
 * `KeyAdapter`'s `keyTyped()`. This should be reported.
 * 
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class BadlyOverriddenKeyAdapter extends java.awt.event.KeyAdapter {

    public void keyTyped() {
    }
}
