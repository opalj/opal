/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ImmutableClassInheritsMutableClass;

import ImmutableClassInheritsMutableClass.JCIPAnnotatedInheritingFromMutable;
import net.jcip.annotations.Immutable;

/**
 * Immutable class inherits from another immutable class. Should not be reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithoutInheritance extends JCIPAnnotatedInheritingFromMutable {

}
