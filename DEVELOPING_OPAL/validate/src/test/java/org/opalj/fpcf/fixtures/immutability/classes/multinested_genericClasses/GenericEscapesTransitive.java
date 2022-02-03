/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

@NonTransitivelyImmutableType(value = "has only deep immutable fields and is not extensible",
analyses =  L1TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value = "has only deep immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
final class ClassWithGenericField {

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private SimpleGenericClass<SimpleMutableClass, FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass>
                    (new SimpleMutableClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DependentlyImmutableClass(value = "has only dependent immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
class SimpleGenericClass<A,B,C> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "field has an immutable field reference and a generic type",
    analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private A a;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "field has an immutable field reference and a generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private B b;

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "field has an immutable field reference and a generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "field is effectively final",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "field is effectively final",
            analyses = L3FieldAssignabilityAnalysis.class)
    private C c;

    SimpleGenericClass(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

@TransitivelyImmutableClass(value = "class has no fields", analyses = L0FieldImmutabilityAnalysis.class)
class FinalEmptyClass{

}

@MutableType(value = "class is mutable",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has a mutable instance field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class SimpleMutableClass{

    @MutableField(value = "field is public", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value= "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public int n = 10;
}