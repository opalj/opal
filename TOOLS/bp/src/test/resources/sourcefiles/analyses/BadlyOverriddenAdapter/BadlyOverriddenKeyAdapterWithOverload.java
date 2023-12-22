/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * This class inherits from `KeyAdapter` but does not override its `keyTyped()` properly,
 * because the signatures of both `keyTyped()` overloads here are incompatible to that
 * of `KeyAdapter`'s `keyTyped()`. This should be reported.
 * 
 * @author Daniel Klauer
 */
public class BadlyOverriddenKeyAdapterWithOverload extends java.awt.event.KeyAdapter {

    public void keyTyped() {
    }

    public void keyTyped(float arg0) {
    }
}
