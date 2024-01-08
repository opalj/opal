/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BoxingImmediatelyUnboxedToPerformCoercion;

/**
 * Some class that does not do any boxing/unboxing and should not be reported.
 * 
 * @author Daniel Klauer
 */
public class UnrelatedClass {

    void test() {
        System.out.println("test\n");
    }
}
