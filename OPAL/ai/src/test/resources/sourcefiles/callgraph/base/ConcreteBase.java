/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.base;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
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
