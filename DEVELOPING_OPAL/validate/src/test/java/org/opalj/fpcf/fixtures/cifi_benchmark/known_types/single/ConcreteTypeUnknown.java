/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.known_types.single;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

/**
 * This class represents the counter-example in which the type of the object assigned to a field is not known
 */
//@Immutable
@NonTransitivelyImmutableType("class is non transitively immutable and final")
@NonTransitivelyImmutableClass("class has only one non-transitively immutable field")
final class ConcreteTypeUnknown {

    //@Immutable
    @NonTransitivelyImmutableField("field has a mutable type")
    @NonAssignableField("field is final")
    private final CustomObject customObject;

    public ConcreteTypeUnknown(CustomObject customObject) {
        this.customObject = customObject;
    }
}