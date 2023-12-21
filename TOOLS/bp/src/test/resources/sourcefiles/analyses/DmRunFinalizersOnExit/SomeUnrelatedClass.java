/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package DmRunFinalizersOnExit;

/**
 * Some class that does not call System.runFinalizersOnExit() and shouldn't be an issue
 * for the analysis.
 * 
 * @author Daniel Klauer
 */
public class SomeUnrelatedClass {

    void test() {
        System.out.println("test\n");
    }
}
