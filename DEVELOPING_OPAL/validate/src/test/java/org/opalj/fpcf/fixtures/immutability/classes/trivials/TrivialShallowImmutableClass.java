/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

@MutableType("Because of not final class")
@NonTransitivelyImmutableClass("has shallow immutable field")
public class TrivialShallowImmutableClass {

    @NonTransitivelyImmutableField(value = "Because object can not escape", analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField("Because it is private")
    private TrivialMutableClass mutableClass = new TrivialMutableClass();
}

@MutableType("Because of not final mutable class")
@MutableClass("Because of mutable field")
class TrivialMutableClass {

    @MutableField("Because of mutable reference")
    @AssignableField("Because of public field")
    public int n = 0;
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@TransitivelyImmutableClass(value = "class has no fields", analyses = L1ClassImmutabilityAnalysis.class)
class EmptyClass {
}

@TransitivelyImmutableType(value = "class has no fields and is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@TransitivelyImmutableClass(value = "class has no fields", analyses = L1ClassImmutabilityAnalysis.class)
final class FinalEmptyClass {
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DependentlyImmutableClass(value = "class has dependent immutable field", analyses = L1ClassImmutabilityAnalysis.class)
class TrivialDependentImmutableClass<T> {

    @DependentlyImmutableField(value = "Because of type generic type T", analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "Private effectively final field",
            analyses = L3FieldAssignabilityAnalysis.class)
    private T t;

    public TrivialDependentImmutableClass(T t){
        this.t = t;
    }
}

@MutableType(value = "Because of not final class",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@TransitivelyImmutableClass(value = "Generic Type is not used as field Type", analyses = L1ClassImmutabilityAnalysis.class)
class GenericTypeIsNotUsedAsFieldType<T> {

    @TransitivelyImmutableField(value="effective immutable field with primitive type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value="effective immutable field", analyses = L3FieldAssignabilityAnalysis.class)
    private int n = 0;

    GenericTypeIsNotUsedAsFieldType(T t){
        String s = t.toString();
    }
}

@MutableType(value = "Because of mutable not final class",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "Generic class but public field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class ClassWithGenericPublicField<T> {

    @MutableField(value = "Because of mutable reference", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "Field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public T t;

    ClassWithGenericPublicField(T t){
        this.t = t;
    }
}
