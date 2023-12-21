/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ImmutableClassInheritsMutableClass;

import ImmutableClassInheritsMutableClass.MutableClass;
import net.jcip.annotations.Immutable;

/**
 * In and off itself immutable class inherits from a mutable class. Thus it should be
 * reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedInheritingFromMutable extends MutableClass {

}
