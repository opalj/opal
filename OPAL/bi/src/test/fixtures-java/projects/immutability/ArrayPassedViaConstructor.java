/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.ConditionallyImmutable;;

/**
 * A conditionally immutable class which references an array which is set via the constructor.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("the reference to the array is just an alias")
public class ArrayPassedViaConstructor {

    private final int[] array;

    public ArrayPassedViaConstructor(int[] array) {
        this.array = array;
    }

    public int getElement(int index) {
        if (index >= 0 && index < array.length) {
            return array[index];
        }
        return -1;
    }
}
