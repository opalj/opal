/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package NativeMethodInImmutableClass;

/**
 * Some class with public fields, but without an immutable annotation. This should not be
 * reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
public class NotImmutableWithPublicFields {

    public int x;
    public int[] foo;

    public NotImmutableWithPublicFields(int arg0, int[] arg1) {
        x = arg0;
        foo = arg1;
    }

    public NotImmutableWithPublicFields() {
        x = 5;
        int[] temp = { 1, 2, 3, 4 };
        foo = temp;
    }
}
