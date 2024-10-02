/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * A class that references itself during <clinit>, and should not trigger reports.
 * 
 * @author Daniel Klauer
 */
public class ClassUsesItself {

    public final static String text = "hello";

    static {
        // A GETSTATIC instruction during the static initializer.
        System.out.println(text);
    }
}
