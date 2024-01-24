/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldShouldBePackageProtected;

import java.util.Hashtable;

/**
 * @author Daniel Klauer
 */
public class VariousFields {

    /**
     * Both fields already are package-protected, no report.
     */
    static final int[] array1 = new int[10];
    static final Hashtable<String, Integer> hashtb1 = new Hashtable<String, Integer>();

    /**
     * Both fields are public, so they should be reported.
     */
    public static final int[] array2 = new int[10];
    public static final Hashtable<String, Integer> hashtb2 = new Hashtable<String, Integer>();

    /**
     * Both fields have primitive types that the analysis does not check, so no report.
     */
    public static int int1 = 123;
    public static String s1 = "abc";
}
