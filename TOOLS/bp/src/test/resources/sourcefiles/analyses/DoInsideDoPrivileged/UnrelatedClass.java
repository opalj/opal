/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package DoInsideDoPrivileged;

/**
 * A class without setAccessible() calls. It should not be reported.
 * 
 * @author Daniel Klauer
 */
public class UnrelatedClass {

    void test() {
        System.out.println("test\n");
    }
}
