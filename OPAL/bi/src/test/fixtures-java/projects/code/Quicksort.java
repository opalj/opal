/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package code;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class Quicksort {

    static int partition(double[] a, int left, int right) {

        int i = left - 1;
        int j = right;
        while (true) {
            while (a[++i] < a[right]) {
                /*empty*/
            }
            while (a[right] < a[--j])
                if (j == left)
                    break;
            if (i >= j)
                break;
            exch(a, i, j);
        }
        exch(a, i, right);
        return i;
    }

    private static void exch(double[] a, int i, int j) {

        // Not needed

    }

}
