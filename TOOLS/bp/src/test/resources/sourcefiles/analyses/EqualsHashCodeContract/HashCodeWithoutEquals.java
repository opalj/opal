/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package EqualsHashCodeContract;

/**
 * Class with a hashCode() method but no equals() method. This breaks the equals/hashCode
 * contract.
 * 
 * @author Daniel Klauer
 */
public class HashCodeWithoutEquals {

    @Override
    public int hashCode() {
        return 0;
    }
}
