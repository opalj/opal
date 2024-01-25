/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CatchesIllegalMonitorStateException;

/**
 * Some class that has nothing to do with `IllegalMonitorStateException`s: It neither
 * catches them nor has any `wait()` or `notifyAll()` calls that might trigger them.
 * 
 * @author Daniel Klauer
 */
public class UnrelatedClass {

    void test() {
        System.out.println("test\n");
    }
}
