/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UnusedPrivateFields;

/**
 * A class containing various unused private fields.
 * 
 * @author Daniel Klauer
 */
@SuppressWarnings("unused")
public class Unused {

    // Normal field without initializer
    private int a;

    // Normal field with a constant initializer
    private int b = 111;

    // Final field with non-constant initializer
    private final Unused c = new Unused();

    // Final field with another constant initializer
    private final int d = 222;
}
