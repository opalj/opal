package org.opalj.fpcf.fixtures.immutability.sandbox32;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

final class DeepGeneric {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle generics",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "only deep immutable types in generics", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1;

    public DeepGeneric(GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1){
        this.gc1 = gc1;
    }

}

@ShallowImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@ShallowImmutableClass(value = "can not handle generics", analyses = L0ClassImmutabilityAnalysis.class)
@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericBaseClass<T1,T2,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value="can not work with generic type",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value="effective final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T3 t3;

    public GenericBaseClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

}

final class FinalClassWithoutFields{}