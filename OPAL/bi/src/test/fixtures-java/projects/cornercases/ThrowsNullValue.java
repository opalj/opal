/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package cornercases;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class ThrowsNullValue {

    public static void main(String[] args) {
        try {
            Exception e = null;
            if (args.length == 0)
                e = new RuntimeException();

            throw e;
        } catch (Exception e) {
            // empty
        }
    }

}
