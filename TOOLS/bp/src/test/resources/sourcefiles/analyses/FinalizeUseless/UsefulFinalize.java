/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FinalizeUseless;

/**
 * A class with a `finalize()` that does more than just call `super.finalize()`. This
 * should not be reported.
 * 
 * @author Roberts Kolosovs
 */
public class UsefulFinalize {

    @Override
    protected void finalize() throws Throwable {
        System.out.println("So I will be running finalize now.");
        System.out.println("Just so you are informed.");
        System.out.println("Ok, finalize is about to run.");
        System.out.println("I don't want to die...");
        super.finalize();
    }
}
