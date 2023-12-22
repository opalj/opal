/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * The analysis must be able to deal with recursive calls when analyzing called methods,
 * to avoid infinite recursion.
 * 
 * @author Daniel Klauer
 */
public class Recursion {

    static void limitedRecursion(int i) {
        if (i < 10) {
            limitedRecursion(i + 1);
        }
        System.out.println(RecursionSubclass.i1);
    }

    static void infiniteRecursion() {
        infiniteRecursion();
        System.out.println(RecursionSubclass.i2);
    }

    static {
        limitedRecursion(0);
        infiniteRecursion();
    }
}

class RecursionSubclass extends Recursion {

    public static int i1 = 123;
    public static int i2 = 123;
}
