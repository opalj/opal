/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * A mutable class which has a method that throws an exception after calling it 10 times.
 * 
 * @author Andre Pacak
 */
@Mutable("throws exception in method")
public class ThrowException {

    private int x = 0;

    public int getOne() throws Exception {
        x++;
        if (x >= 10) {
            throw new Exception();
        }
        return 1;
    }
}
