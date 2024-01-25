/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `MouseInputAdapter` but does not override `mouseClicked()`
 * properly, because the signature of the `mouseClicked()` here is not compatible to that
 * of `MouseInputAdapter`'s `mouseClicked()`. This should be reported.
 * 
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class BadlyOverriddenMouseInputAdapter extends javax.swing.event.MouseInputAdapter {

    public void mouseClicked(boolean arg0) {
    }
}
