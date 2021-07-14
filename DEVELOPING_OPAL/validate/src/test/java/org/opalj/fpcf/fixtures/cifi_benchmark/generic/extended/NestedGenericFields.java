package org.opalj.fpcf.fixtures.cifi_benchmark.generic.extended;

import org.opalj.fpcf.fixtures.cifi_benchmark.general.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.cifi_benchmark.general.FinalClassWithNoFields;
import org.opalj.fpcf.fixtures.cifi_benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

/**
 * Class represents different cases of nested genericity.
 */
//import edu.cmu.cs.glacier.qual.Immutable;
public class NestedGenericFields<T> {

    //@Immutable
    @TransitivelyImmutableField("The generic types are nested transitively immutable")
    @NonAssignableField("field is final")
    private final Generic<Generic<FinalClassWithNoFields>> nestedTransitivelyImmutable =
            new Generic<>(new Generic<>(new FinalClassWithNoFields()));

    //@Immutable
    @DependentlyImmutableField("")
    @EffectivelyNonAssignableField("field is final")
    private Generic<Generic<T>> nestedMutable;

    //@Immutable
    @DependentlyImmutableField(value = "only generic typ parameters", parameter={"T"})
    @NonAssignableField("field is final")
    private final Generic<Generic<T>> nestedDependent;

    //@Immutable
    @NonTransitivelyImmutableField("Only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final Generic<Generic<ClassWithMutableFields>> nestedNonTransitive =
            new Generic<>(new Generic<>(new ClassWithMutableFields()));

    public void setNestedMutable(Generic<Generic<T>> nestedMutable){
        this.nestedMutable = nestedMutable;
    }

    public NestedGenericFields(T t){
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }
}
