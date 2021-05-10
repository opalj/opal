/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

@NonTransitiveImmutableType(value = "has only deep immutable fields and is not extensible",
analyses =  L1TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "has only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class ClassWithGenericField {

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private SimpleGenericClass<SimpleMutableClass, FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass>
                    (new SimpleMutableClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DependentlyImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
class SimpleGenericClass<A,B,C> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "field has an immutable field reference and a generic type",
    analyses = L3FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private A a;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "field has an immutable field reference and a generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private B b;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DependentImmutableField(value = "field has an immutable field reference and a generic type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private C c;

    SimpleGenericClass(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

@TransitivelyImmutableClass(value = "class has no fields", analyses = L3FieldImmutabilityAnalysis.class)
class FinalEmptyClass{

}

@MutableType(value = "class is mutable",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has a mutable instance field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class SimpleMutableClass{

    @MutableField(value = "field is public", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value= "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public int n = 10;
}