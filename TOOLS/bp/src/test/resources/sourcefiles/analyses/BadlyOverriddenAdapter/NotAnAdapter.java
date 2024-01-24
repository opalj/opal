/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BadlyOverriddenAdapter;

/**
 * Some class that is not related to any *Adapter classes at all. No problems should be
 * detected here.
 * 
 * @author Daniel Klauer
 */
public class NotAnAdapter {

    void test() {
        System.out.println("test\n");
    }
}
