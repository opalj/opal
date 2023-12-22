/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BitNops;

/**
 * Some AND/OR no-ops involving a `public` field as operand. The field may be altered at
 * runtime, so our static analyses cannot detect any issues here.
 * 
 * @author Daniel Klauer
 */
public class InvolvingPublicField {

    public int zero = 0;

    void test(int a) {
        System.out.println(a | zero);
        System.out.println(a & zero);
    }
}
