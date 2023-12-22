/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * This class deliberately contains "useless" code to test checkers that search for it.
 * 
 * It contains: - a finalize method that just calls super.finalize();
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class UnrelatedControlClassB {

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    static int blubb = 5;
}
