package org.opalj.fpcf.fixtures.benchmark.generic.extended;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.benchmark.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitiveImmutableType;

/**
 * Class represents different cases of nested genericity
 */
//import edu.cmu.cs.glacier.qual.Immutable;
public class NestedGenericFields<T> {

    //@Immutable
    @TransitiveImmutableField("")
    @NonAssignableFieldReference("field is final")
    private final Generic<Generic<FinalClassWithNoFields>> nestedTransitive =
            new Generic<>(new Generic<>(new FinalClassWithNoFields()));

    //@Immutable
    @MutableField("")
    @NonAssignableFieldReference("field is final")
    private Generic<Generic<T>> nestedMutable;

    //@Immutable
    @DependentImmutableField("only generic typ parameters")
    @NonAssignableFieldReference("field is final")
    private final Generic<Generic<T>> nestedDependent;

    //@Immutable
    @NonTransitivelyImmutableField("Only transitively immutable type parameters")
    @NonAssignableFieldReference("field is final")
    private final Generic<Generic<ClassWithMutableFields>> nestedNonTransitive =
            new Generic<>(new Generic<>(new ClassWithMutableFields()));

    public void setNestedMutable(Generic<Generic<T>> nestedMutable){
        this.nestedMutable = nestedMutable;
    }

    public NestedGenericFields(T t){
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }
}

@TransitiveImmutableType("")
final class FinalClassWithNoFields{}

