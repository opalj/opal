/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.known_types.single;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the case in which a single known object is assigned to a field.
 */
//@Immutable
@MutableType("class is not final")
@TransitiveImmutableClass("class has only one transitive immutable field")
class ConcreteObjectInstanceAssigned {

    //@Immutable
    @TransitiveImmutableField("concrete object is known")
    @NonAssignableFieldReference("the field is final")
    private final Object object = new Object();

    public Object getObject() {
        return this.object;
    }

}


