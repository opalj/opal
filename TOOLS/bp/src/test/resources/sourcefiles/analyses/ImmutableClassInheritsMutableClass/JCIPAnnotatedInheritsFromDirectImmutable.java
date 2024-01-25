/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ImmutableClassInheritsMutableClass;

import net.jcip.annotations.Immutable;

/**
 * Immutable class inheriting from an immutable class. Should not be reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedInheritsFromDirectImmutable extends JCIPAnnotatedImmutable {

}
