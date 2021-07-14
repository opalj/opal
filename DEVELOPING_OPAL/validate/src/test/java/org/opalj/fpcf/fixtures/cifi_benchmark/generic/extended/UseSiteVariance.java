/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.generic.extended;
import org.opalj.fpcf.fixtures.cifi_benchmark.common.ClassWithMutableField;
import org.opalj.fpcf.fixtures.cifi_benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;


/**
 * This class encompasses different cases of use site variance.
 */
//@Immutable
@MutableType("class is not final")
@NonTransitivelyImmutableClass("Class has a non transitively immutable field")
class UseSiteVariance<T extends ClassWithMutableField> {

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

    public UseSiteVariance(Generic<? super EmptyClass> nonTransitivelyImmutableField,
                           Generic<? extends FinalEmptyClass> transitivelyImmutableField, T t){
        this.t = t;
        this.nonTransitivelyImmutableField = nonTransitivelyImmutableField;
        this.transitivelyImmutableField = transitivelyImmutableField;
    }

    //@Immutable
    @MutableType("Class is not final")
    @TransitivelyImmutableClass("Class with no fields")
    class EmptyClass {}

    //@Immutable
    @TransitivelyImmutableType("Class is final, has no fields and extends a transitively immutable class")
    @TransitivelyImmutableClass("empty")
    final class FinalEmptyClass extends EmptyClass {}
}


