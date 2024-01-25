/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package EqualsHashCodeContract;

/**
 * Class with an equals() method but no hashCode() method. This breaks the equals/hashCode
 * contract.
 * 
 * @author Daniel Klauer
 */
public class EqualsWithoutHashCode {

    @Override
    public boolean equals(Object other) {
        return false;
    }
}
