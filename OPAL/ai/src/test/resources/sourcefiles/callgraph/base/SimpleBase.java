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
public class SimpleBase implements Base {

    @Override
    public void interfaceMethod() {
        // empty
    }

    @Override
    public void abstractMethod() {
        // empty
    }

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

    @Override
    public String toString() {
        return "SimpleBase";
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
