/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * This class is used by some other class files as a foundation for some more elaborated
 * tests.
 *
 * @author Marco Jacobasch
 */
public abstract class AbstractBase implements Base {

    public final String string;

    public AbstractBase() {
        this.string = "abstract";
    }

    public AbstractBase(String s) {
        this.string = s;
    }

    @Override
    public void interfaceMethod() {
        // empty
    }

    public abstract void abstractMethod();

    @Override
    public void abstractImplementedMethod() {
        // empty
    }

    @Override
    public void implementedMethod() {
        // empty
    }

    public static void staticMethod() {
        // empty
    }

}
