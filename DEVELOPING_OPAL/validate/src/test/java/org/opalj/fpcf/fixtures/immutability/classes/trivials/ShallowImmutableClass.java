/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

@MutableType("Because of not final class")
@DeepImmutableClass("has shallow immutable field")
public class ShallowImmutableClass {

    @DeepImmutableField(value = "Because object can not escape", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference("Because it is private")
    private TrivialMutableClass mutableClass = new TrivialMutableClass();
}

@MutableType("Because of not final mutable class")
@MutableClass("Because of mutable field")
class TrivialMutableClass {

    @MutableField("Because of mutable reference")
    @MutableFieldReference("Because of public field")
    public int n = 0;
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "class has no fields", analyses = L1ClassImmutabilityAnalysis.class)
class EmptyClass {
}

@DeepImmutableType(value = "class has no fields and is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@DeepImmutableClass(value = "class has no fields", analyses = L1ClassImmutabilityAnalysis.class)
final class FinalEmptyClass {
}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DependentImmutableClass(value = "class has dependent immutable field", analyses = L1ClassImmutabilityAnalysis.class)
class TrivialDependentImmutableClass<T> {

    @DependentImmutableField(value = "Because of type generic type T", analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "Private effectively final field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private T t;

    public TrivialDependentImmutableClass(T t){
        this.t = t;
    }
}

@MutableType(value = "Because of not final class",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "Generic Type is not used as field Type", analyses = L1ClassImmutabilityAnalysis.class)
class GenericTypeIsNotUsedAsFieldType<T> {

    @DeepImmutableField(value="effective immutable field with primitive type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value="effective immutable field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
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

    @MutableField(value = "Because of mutable reference", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "Field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public T t;

    ClassWithGenericPublicField(T t){
        this.t = t;
    }
}
