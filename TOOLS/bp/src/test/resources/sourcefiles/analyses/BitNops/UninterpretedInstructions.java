/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BitNops;

/**
 * A class with a method containing instructions that AI won't reach when using precise
 * integer values. BitNops should not crash due to this.
 * 
 * @author Daniel Klauer
 */
public class UninterpretedInstructions {

    public void test() {
        int i = 0;
        if (i == 0) {
        } else {
            // Unreachable code that won't be analyzed by AI
            // (won't have operands information for these PCs)
            i = i & 0;
        }
    }
}
