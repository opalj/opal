/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package PublicFinalizeMethodShouldBeProtected;

/**
 * This class contains a `public` `finalize()` method. Because it is `public`, it can be
 * called from the outside, at any time, which is not the intention of `finalize()`. This
 * should be reported.
 * 
 * @author Daniel Klauer
 */
public class PublicFinalizeMethod {

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }
}
