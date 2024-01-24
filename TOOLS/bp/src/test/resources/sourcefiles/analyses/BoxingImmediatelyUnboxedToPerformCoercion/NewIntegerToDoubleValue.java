/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BoxingImmediatelyUnboxedToPerformCoercion;

/**
 * @author Daniel Klauer
 */
public class NewIntegerToDoubleValue {

    void test() {
        double d = new Integer(123).doubleValue();
        System.out.println(d);
    }
}
