/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SerializableNoSuitableConstructor;

/**
 * This is a superclass of SerializableInheritsFromNoSuitableConstructor, which implements
 * Serializable, but this superclass does not have a constructor with no arguments. Thus
 * it should be reported.
 * 
 * @author Roberts Kolosovs
 */
public class SerializableNoSuitableConstructor {

    public SerializableNoSuitableConstructor(int[] args) {
    }
}
