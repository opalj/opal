/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package DoInsideDoPrivileged;

/**
 * Invokes setAccessible outside of doPriviledged.
 * 
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class CallsSetAccessibleOutsideDoPrivileged {

    ClassWithAField t = new ClassWithAField();

    public void method() {
        java.lang.reflect.Field[] fs = t.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fs) {
            f.setAccessible(true);
        }
    }
}
