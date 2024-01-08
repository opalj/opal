/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UselessIncrementInReturn;

/**
 * Various methods incrementing the int field during return instructions.
 * 
 * In this case the increment is not useless, even though it happens as part of a return
 * statement, because the resulting value is stored into the field, instead of being
 * thrown away. Thus, none of these cases should trigger reports.
 * 
 * @author Daniel Klauer
 */
public class IntFieldIncrementInReturn {

    int i = 0;

    int standaloneIncrement() {
        i++;
        return i;
    }

    int intToIreturn() {
        return i++;
    }

    long intToLreturn() {
        return i++;
    }

    float intToFreturn() {
        return i++;
    }

    double intToDreturn() {
        return i++;
    }
}
