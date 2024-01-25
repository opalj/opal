/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import java.util.Random;

/**
 * This class enables the iteration over a permutation of the values [0..max).
 * 
 * @author Michael Eichberg
 */
public class Permutation {

    private static final Random random = new Random(System.currentTimeMillis());

    private final int[] permutation;

    private int index = 0;

    public Permutation(int max) {

        this.permutation = new int[max];
        for (int i = 0; i < max; i++)
            permutation[i] = i;

        // Creates a permutation of the values stored in the array by shuffling the values.
        // This algorithm only requires c*n steps (n = size of the array).

        for (int i = 0; i < max; i++) {
            int targetSlot = random.nextInt(max);
            int tmp = permutation[targetSlot];
            permutation[targetSlot] = permutation[i];
            permutation[i] = tmp;
        }
    }

    public boolean hasNext() {

        return index < permutation.length;
    }

    public int next() throws ArrayIndexOutOfBoundsException {

        return permutation[index++];
    }
}
