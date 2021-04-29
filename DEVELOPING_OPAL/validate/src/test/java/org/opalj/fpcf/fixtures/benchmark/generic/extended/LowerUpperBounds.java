/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitiveImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;

class LowerUpperBounds<T extends ClassWithMutableFields> {

    //@Immutable
    @NonTransitivelyImmutableField("type T extends a mutable type")
    @NonAssignableFieldReference("field is final")
    private final T t;

    //@Immutable
    @NonTransitivelyImmutableField("has super type object")
    @NonAssignableFieldReference("field is final")
    private final Generic<? super EmptyClass> g1;

    //@Immutable
    @NonTransitivelyImmutableField("type can still be non-transitive")
    @NonAssignableFieldReference("field is final")
    private final Generic<? extends EmptyClass> g2;

    //@Immutable
    @TransitiveImmutableField("Type can only be final")
    @NonAssignableFieldReference("field is final")
    private final Generic<? extends FinalEmptyClass> g3;

    //@Immutable
    @NonTransitivelyImmutableField("has super type object")
    @NonAssignableFieldReference("field is final")
    private final Generic<? super FinalEmptyClass> g4;

    public LowerUpperBounds(Generic<? super EmptyClass> g1, Generic<? extends EmptyClass> g2, Generic<? extends FinalEmptyClass> g3, Generic<? super FinalEmptyClass> g4, T t){
        this.g1 = g1;
        this.g2 = g2;
        this.g3 = g3;
        this.g4 = g4;
        this.t = t;
    }

    @MutableType("not final")
    @TransitiveImmutableClass("empty")
    class EmptyClass {}

    @MutableType("not final")
    @TransitiveImmutableClass("empty")
    class X2 extends EmptyClass {}

    @TransitiveImmutableType("final")
    @TransitiveImmutableClass("empty")
    final class FinalEmptyClass extends EmptyClass {}
}


