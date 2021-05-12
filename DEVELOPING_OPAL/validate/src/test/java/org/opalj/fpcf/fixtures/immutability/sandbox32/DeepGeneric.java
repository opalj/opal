package org.opalj.fpcf.fixtures.immutability.sandbox32;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

final class DeepGeneric {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle generics",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @TransitivelyImmutableField(value = "only deep immutable types in generics", analyses = L0FieldImmutabilityAnalysis.class)
    @NonAssignableFieldReference(value = "effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1;

    public DeepGeneric(GenericBaseClass<FinalClassWithoutFields,FinalClassWithoutFields,FinalClassWithoutFields> gc1){
        this.gc1 = gc1;
    }

}

@NonTransitiveImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "can not handle generics", analyses = L0ClassImmutabilityAnalysis.class)
@DependentImmutableType(value = "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class GenericBaseClass<T1,T2,T3> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @NonAssignableFieldReference(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T1 t1;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @NonAssignableFieldReference(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T2 t2;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @NonAssignableFieldReference(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T3 t3;

    public GenericBaseClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

}

final class FinalClassWithoutFields{}