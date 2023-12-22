/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UselessIncrementInReturn;

/**
 * Various methods containing int increment and return instructions. The analysis should
 * only report those cases where the increment appears in the return and thus is dead
 * code.
 * 
 * @author Daniel Klauer
 */
public class IntParameterIncrementInReturn {

    int standaloneIncrement(int i) {
        i++; // should not be reported
        return i;
    }

    int intToIreturn(int i) {
        return i++; // should be reported
    }

    long intToLreturn(int i) {
        return i++; // should be reported
    }

    float intToFreturn(int i) {
        return i++; // should be reported
    }

    double intToDreturn(int i) {
        return i++; // should be reported
    }
}
