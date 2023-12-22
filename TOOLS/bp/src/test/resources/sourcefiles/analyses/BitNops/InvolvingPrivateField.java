/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BitNops;

/**
 * Some AND/OR no-ops involving a `private` field as operand. Currently BitNops does not
 * detect this case, but that may change in the future, if it can determine that the field
 * is never written.
 * 
 * @author Daniel Klauer
 */
public class InvolvingPrivateField {

    private int zero = 0;

    void test(int a) {
        System.out.println(a | zero);
        System.out.println(a & zero);
    }
}
