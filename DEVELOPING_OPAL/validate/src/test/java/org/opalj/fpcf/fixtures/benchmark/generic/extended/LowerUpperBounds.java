/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;


/**
 * This class encompasses different cases where the generic type has a lower/upper bound.
 */
@MutableType("class is not final")
@NonTransitivelyImmutableClass("Class has a non transitively immutable field")
class LowerUpperBounds<T extends ClassWithMutableFields> {

    //@Immutable
    @NonTransitivelyImmutableField("The field type T extends a mutable type")
    @NonAssignableField("field is final")
    private final T t;

    //@Immutable
    @NonTransitivelyImmutableField("? has super type object")
    @NonAssignableField("field is final")
    private final Generic<? super EmptyClass> nonTransitivelyImmutableField;


    //@Immutable
    @TransitivelyImmutableField("Type ? can only be FinalEmptyClass and, thus, transitively immutable")
    @NonAssignableField("field is final")
    private final Generic<? extends FinalEmptyClass> transitivelyImmutableField;

    public LowerUpperBounds(Generic<? super EmptyClass> nonTransitivelyImmutableField,
                            Generic<? extends FinalEmptyClass> transitivelyImmutableField, T t){
        this.t = t;
        this.nonTransitivelyImmutableField = nonTransitivelyImmutableField;
        this.transitivelyImmutableField = transitivelyImmutableField;
    }

    @MutableType("Class is not final")
    @TransitivelyImmutableClass("Class with no fields")
    class EmptyClass {}

    @TransitivelyImmutableType("Class is final, has no fields and extends a transitively immutable class")
    @TransitivelyImmutableClass("empty")
    final class FinalEmptyClass extends EmptyClass {}
}


