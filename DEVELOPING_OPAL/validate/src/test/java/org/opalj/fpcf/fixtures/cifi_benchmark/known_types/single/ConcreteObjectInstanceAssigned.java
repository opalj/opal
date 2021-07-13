/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.known_types.single;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the case in which a single known object is assigned to a field.
 */
//@Immutable
@MutableType("Class is not final")
@NonTransitivelyImmutableClass("Class has at least transitive immutable field")
class ConcreteObjectInstanceAssigned {

    //@Immutable
    @TransitivelyImmutableField("Field has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final Integer integer = new Integer(5);

    //@Immutable
    @NonTransitivelyImmutableField("Field has a mutable type")
    @NonAssignableField("Field is final")
    private final MutableClass mutableClass = new MutableClass();

    //@Immutable
    @TransitivelyImmutableField("concrete object is known")
    @NonAssignableField("The field is final")
    private final TransitivelyImmutableClass transitivelyImmutableClass = new TransitivelyImmutableClass();

    public Object getTransitivelyImmutableClass() {
        return this.transitivelyImmutableClass;
    }

    //@Immutable
    private final Object managedObjectManagerLock = new CustomObject();

    //@Immutable
    @NonTransitivelyImmutableField("all concrete objects that can be assigned are not known")
    private TransitivelyImmutableClass fieldWithMutableType = new TransitivelyImmutableClass();


    //The many constructors are needed to simulate the case of multiple constructors
    public ConcreteObjectInstanceAssigned(int n){}

    public ConcreteObjectInstanceAssigned(char c){}

    public ConcreteObjectInstanceAssigned(String s){}

    public ConcreteObjectInstanceAssigned(TransitivelyImmutableClass transitivelyImmutableClass) {
        this.fieldWithMutableType = transitivelyImmutableClass;
    }
}

//@Immutable
class MutableClass {
    public int n = 8;
}

//@Immutable
class TransitivelyImmutableClass {
}
