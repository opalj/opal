/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `KeyAdapter` and properly overrides its `keyTyped()`. No
 * report should be generated for this.
 * 
 * @author Florian Brandherm
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class CorrectlyOverriddenKeyAdapter extends java.awt.event.KeyAdapter {

    @Override
    public void keyTyped(java.awt.event.KeyEvent event) {
    }
}
