/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `KeyAdapter` and the 1st `keyTyped()` method here properly
 * overrides `KeyAdapter`'s `keyTyped()`. No report should be generated for this.
 * 
 * @author Daniel Klauer
 */
public class Overload1CorrectlyOverridesKeyAdapter extends java.awt.event.KeyAdapter {

    @Override
    public void keyTyped(java.awt.event.KeyEvent event) {
    }

    public void keyTyped(float arg0) {
    }
}
