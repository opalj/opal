/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.ConditionallyImmutable;

/**
 * A conditionally immutable class which contains an array that is returned by a public
 * method and which is not cloned.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("the internal array is returned by the getArray method")
public class ReturnNonCloneArray {

    private final int[] array = new int[10];

    public int[] getArray() {
        return array;
    }
}
