/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Immutable;

/**
 * An immutable class which references an array that is set via a constructor and which
 * is cloned.
 * 
 * @author Andre Pacak
 */
@Immutable("the given array is cloned before it is assigned to a field and a clone is returned")
public class ClonePassedArray {

    private final int[] array;

    public ClonePassedArray(int[] array) {
        this.array = array.clone();
    }

    public int[] getArray() {
        return array.clone();
    }
}
