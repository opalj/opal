/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package DoInsideDoPrivileged;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Invokes setAccessible inside of doPrivileged.
 * 
 * @author Florian Brandherm
 */
public class CallsSetAccessibleInsideDoPrivileged {

    ClassWithAField t = new ClassWithAField();

    public void method() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                java.lang.reflect.Field[] fs = t.getClass().getDeclaredFields();
                for (java.lang.reflect.Field f : fs) {
                    f.setAccessible(true);
                }
                return null;
            }
        });
    }
}
