/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package LongBitsToDoubleInvokedOnInt;

/**
 * A call to Double.longBitsToDouble() with long argument. This is the correct usage and
 * should not be reported.
 * 
 * @author Daniel Klauer
 */
public class LongBitsToDouble {

    void test() {
        long l = 0xAAAABBBBCCCCDDDDl;
        double d = Double.longBitsToDouble(l);
        System.out.println(d);
    }
}
