/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * A mutable class which defines no additional methods or fields but extends a mutable
 * class which has a public non-final field.
 *
 * @author Andre Pacak
 */
@Mutable("the superclass is mutable")
public class ExtendMutableClass extends NonFinalPublicClass {
    
}
