package org.opalj.fpcf.fixtures.benchmark.generic.extended;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * Class represents different cases of nested genericity
 */
//import edu.cmu.cs.glacier.qual.Immutable;
public class NestedGenericFields<T> {

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("field is final")
    private final Generic<Generic<FinalClassWithNoFields>> nestedTransitive =
            new Generic<>(new Generic<>(new FinalClassWithNoFields()));

    //@Immutable
    @MutableField("")
    @EffectivelyNonAssignableField("field is final")
    private Generic<Generic<T>> nestedMutable;

    //@Immutable
    @DependentImmutableField(value = "only generic typ parameters", parameter={"T"})
    @EffectivelyNonAssignableField("field is final")
    private final Generic<Generic<T>> nestedDependent;

    //@Immutable
    @NonTransitivelyImmutableField("Only transitively immutable type parameters")
    @EffectivelyNonAssignableField("field is final")
    private final Generic<Generic<ClassWithMutableFields>> nestedNonTransitive =
            new Generic<>(new Generic<>(new ClassWithMutableFields()));

    public void setNestedMutable(Generic<Generic<T>> nestedMutable){
        this.nestedMutable = nestedMutable;
    }

    public NestedGenericFields(T t){
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }
}

@TransitivelyImmutableType("")
final class FinalClassWithNoFields{}

