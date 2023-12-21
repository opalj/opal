/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ImmutableClassInheritsMutableClass;

/**
 * Some class with public Fields, but without an immutable annotation. This should not be
 * reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
public class MutableClass {

    public int x;
    public int[] foo;

    public MutableClass(int arg0, int[] arg1) {
        x = arg0;
        foo = arg1;
    }

    public MutableClass() {
        x = 5;
        int[] temp = { 1, 2, 3, 4 };
        foo = temp;
    };
}
