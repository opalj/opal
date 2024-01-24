/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package NativeMethodInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * This class in annotated as immutable and does not contain native methods. It should not
 * be reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithoutNativeMethod {

    public final int foo = 42;
}
