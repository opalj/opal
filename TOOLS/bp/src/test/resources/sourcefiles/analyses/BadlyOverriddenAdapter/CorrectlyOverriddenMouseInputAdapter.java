/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `MouseInputAdapter` and correctly overrides its
 * `mouseClicked()` method. No report should be generated for this.
 * 
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class CorrectlyOverriddenMouseInputAdapter extends
        javax.swing.event.MouseInputAdapter {

    @Override
    public void mouseClicked(java.awt.event.MouseEvent event) {
    }
}
