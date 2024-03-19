/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SerializableNoSuitableConstructor;

/**
 * This class implements java.io.Serializable and inherits from
 * SerializableNoSuitableConstructor which does not have a zero-argument constructor,
 * which however is required for Serializable. The superclass is supposed to be reported,
 * but not this subclass.
 * 
 * @author Roberts Kolosovs
 */
class SerializableInheritsFromNoSuitableConstructor extends
        SerializableNoSuitableConstructor implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public SerializableInheritsFromNoSuitableConstructor(int[] args) {
        super(args);
    }
}
