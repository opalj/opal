/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.known_types.single;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the case in which a single known object is assigned to a field.
 */
//@Immutable
@MutableType("class is not final")
@NonTransitivelyImmutableClass("class has only one transitive immutable field")
class ConcreteObjectInstanceAssigned {

    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final Integer i = new Integer(5);

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final MutableClass mc = new MutableClass();

    //@Immutable
    @TransitivelyImmutableField("concrete object is known")
    @NonAssignableFieldReference("the field is final")
    private final Object object = new Object();

    public Object getObject() {
        return this.object;
    }

    private final Object managedObjectManagerLock = new Object();

    public ConcreteObjectInstanceAssigned(int n){}

    public ConcreteObjectInstanceAssigned(char c){}

    public ConcreteObjectInstanceAssigned(String s){}

    @NonTransitivelyImmutableField("all concrete objects that can be assigned are not known")
    private TransitivelyImmutableClass transitivelyImmutableClass = new TransitivelyImmutableClass();

    public ConcreteObjectInstanceAssigned(TransitivelyImmutableClass transitivelyImmutableClass) {
        this.transitivelyImmutableClass = transitivelyImmutableClass;
    }
}

class MutableClass {
    public int n = 8;
}

class TransitivelyImmutableClass {
}
