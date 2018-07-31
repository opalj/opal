/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.ConditionallyImmutable;

/**
 * A recursive data structure that is mutable because the referenced fields are mutable.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("the referenced objects are mutable")
public class MutableRecursiveDataStructure {

    public final MutableClass object;
    public final MutableRecursiveDataStructure left, right;

    public MutableRecursiveDataStructure(MutableClass object,
            MutableRecursiveDataStructure left, MutableRecursiveDataStructure right) {
        this.object = object;
        this.left = left;
        this.right = right;
    }
}
