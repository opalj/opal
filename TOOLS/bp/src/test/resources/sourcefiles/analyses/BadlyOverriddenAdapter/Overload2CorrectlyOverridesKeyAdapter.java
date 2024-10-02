/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `KeyAdapter` and the 2nd `keyTyped()` method here properly
 * overrides `KeyAdapter`'s `keyTyped()`. No report should be generated for this.
 * 
 * @author Daniel Klauer
 */
public class Overload2CorrectlyOverridesKeyAdapter extends java.awt.event.KeyAdapter {

    public void keyTyped(float arg0) {
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent event) {
    }
}
