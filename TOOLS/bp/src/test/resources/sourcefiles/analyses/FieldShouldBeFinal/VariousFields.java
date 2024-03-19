/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldShouldBeFinal;

import java.util.Hashtable;

/**
 * @author Daniel Klauer
 */
public class VariousFields {

    /**
     * These fields are final already, and they are just int/String, so they are
     * definitely immutable, and should not be reported.
     */
    public static final int int1 = 123;
    public static final String s1 = "abc";

    /**
     * Both fields are non-final, and could be modified. They should be reported.
     */
    public static int int2 = 123;
    public static String s2 = "abc";

    /**
     * These fields are mutable despite being final, because they are not primitives. Only
     * the reference is final, not the array content, etc. The analysis does not report
     * these though.
     */
    public static final int[] array1 = new int[10];
    public static final Hashtable<String, Integer> hashtb1 = new Hashtable<String, Integer>();

    /**
     * These fields are non-final, but should not be reported because they are not
     * primitives. The analysis explicitly ignores arrays and hashtables to avoid
     * detecting too many false-positives.
     */
    public static int[] array2 = new int[10];
    public static Hashtable<String, Integer> hashtb2 = new Hashtable<String, Integer>();
}
