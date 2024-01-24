/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FinalizeUseless;

/**
 * A class with a useless finalize() method. Since it just calls super.finalize(), it may
 * aswell have been omitted, which would have caused super.finalize() to be called
 * directly.
 * 
 * @author Daniel Klauer
 */
public class UselessFinalize {

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
