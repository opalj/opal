/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BitNops;

/**
 * A common construct for which BitNops should not produce false-positive reports: A loop
 * that "collects" flags into a variable. During the first iteration, the OR operation
 * will be a no-op, but in later iterations it won't be.
 * 
 * @author Daniel Klauer
 */
public class FlagCollectionLoop {

    void test() {
        int x = 0;

        for (int i = 0; i < 10; i++) {
            x |= 1 << i;
        }

        System.out.println(x);
    }
}
