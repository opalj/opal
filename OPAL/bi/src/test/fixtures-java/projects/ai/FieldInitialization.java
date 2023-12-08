/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Various constant field definitions. 
 * 
 * @author Michael Eichberg
 */
public class FieldInitialization {

    public final static int I = 0;
    public final static String s = "s";
    public final static Class<String> cs = String.class;
    public final static Object o = new Object();

    public final int iI = 0;
    public final String is = "s";
    public final Class<String> ics = String.class;
    public final Object io = new Object();

    public final int ciI;
    public final String cis;
    public final Class<String> cics;
    public final Object cio;

    public FieldInitialization() {
        ciI = 0;
        cis = "s";
        cics = String.class;
        cio = new Object();
    }

}
