/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * This class is used by some other class files as a foundation for some more elaborated
 * tests.
 *
 * @author Marco Jacobasch
 */
public class ConcreteBase extends AbstractBase {

    public int integer = 0;
    public static double DOUBLE_FIELD = 4.2d;

    public ConcreteBase() {
        super("concrete");
    }

    public ConcreteBase(String s) {
        super(s);
    }

    public ConcreteBase(String s, int i) {
        super(s);
        this.integer = i;
    }

    @Override
    public void abstractMethod() {
        // empty
    }

    @Override
    public void implementedMethod() {
        // emtpy
    }

    public static void staticMethod() {
        // empty
    }

}
