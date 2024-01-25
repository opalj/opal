/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package PublicFinalizeMethodShouldBeProtected;

/**
 * This class contains a `protected` `finalize()` method. This is ok and should not be
 * reported.
 * 
 * @author Daniel Klauer
 */
public class ProtectedFinalizeMethod {

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
