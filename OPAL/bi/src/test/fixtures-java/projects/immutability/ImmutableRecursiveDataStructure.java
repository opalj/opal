/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * A recursive data structure that is immutable because all (public) fields are final and
 * immutable.
 * 
 * @author Andre Pacak
 */
@Immutable("all referenced objects are immutable")
public class ImmutableRecursiveDataStructure {

    public final ImmutableClass object;
    public final ImmutableRecursiveDataStructure left, right;

    public ImmutableRecursiveDataStructure(ImmutableClass object,
            ImmutableRecursiveDataStructure left, ImmutableRecursiveDataStructure right) {
        this.object = object;
        this.left = left;
        this.right = right;
    }
}
