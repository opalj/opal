/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CloneDoesNotCallSuperClone;

/**
 * Some class without clone() method shouldn't be an issue for the analysis.
 * 
 * @author Daniel Klauer
 */
public class SomeClassWithoutClone {

    void test() {
        System.out.println("test\n");
    }
}
