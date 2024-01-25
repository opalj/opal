/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UnusedPrivateFields;

/**
 * Private fields unused in their own class, but used in an inner class. The analysis has
 * to analyze inner classes too in order to detect this.
 * 
 * @author Daniel Klauer
 */
public class UsedInInnerClass {

    // This field really is unused, it should be reported.
    @SuppressWarnings("unused")
    private final String reallyUnused = "reallyUnused";

    // These fields however aren't unused -- they're used in inner classes, and should not
    // be reported.
    private final String usedByInner = "usedByInner";
    private final String usedByInnerInner = "usedByInnerInner";

    class InnerClass {

        void test() {
            System.out.println(usedByInner);
        }

        class InnerInnerClass {

            void test() {
                System.out.println(usedByInnerInner);
            }
        }

    }
}
