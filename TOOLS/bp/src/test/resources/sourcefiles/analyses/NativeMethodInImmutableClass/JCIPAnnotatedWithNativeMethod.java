/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package NativeMethodInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * This class is annotated as immutable and contains native code. This is a problem, as
 * the native code may change the classes fields in unexpected ways. This should be
 * reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithNativeMethod {

    // Field is immutable because of lack of setter
    private int foo = 42;

    // Returning foo is allowed as it is a java primitive and thus passed by value.
    public int getFoo() {
        return foo;
    }

    // This method may do anything. There is no easy way to know. We assume immutability
    // to be violated.
    public native void changeFoo(int v);

}
