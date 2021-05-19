/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.known_types.single;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;

/**
 * This class represents the counter-example in which the type of the object assigned to a field is not known
 */

//@Immutable
//@NonTransitiveImmutableType("class is non transitive immutable and final")
//@NonTransitivelyImmutableClass("class has only one non transitive immutable field")
final class ConcreteTypeUnknown {

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableField("")
    private final Object object;

    public ConcreteTypeUnknown(Object object) {
        this.object = object;
    }
}