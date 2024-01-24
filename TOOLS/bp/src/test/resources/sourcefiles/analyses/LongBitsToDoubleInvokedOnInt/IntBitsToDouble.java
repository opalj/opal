/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package LongBitsToDoubleInvokedOnInt;

/**
 * A call to `Double.longBitsToDouble()` with `int` argument. This is dangerous; normally
 * `Double.longBitsToDouble()` takes a `long` argument, as a `long` is needed to hold a
 * `double`, while an `int` is too small. This should be reported.
 * 
 * @author Daniel Klauer
 */
public class IntBitsToDouble {

    void test() {
        int i = 0xAABBCCDD;
        double d = Double.longBitsToDouble(i);
        System.out.println(d);
    }
}
