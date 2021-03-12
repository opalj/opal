/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

public class ClassWithProtectedFields {
    @MutableField(value = "the field has a mutable field reference",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "the field is protected",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    protected FinalEmptyClass fec1 = new FinalEmptyClass();

    @MutableField("Because of Mutable Reference")
    @MutableFieldReference(value = "Because it is declared as protected",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    protected ClassWithPublicFields cwpf1 = new ClassWithPublicFields();

    @ShallowImmutableField(value = "field has an immutable reference and mutable type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "Declared final Field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final ClassWithPublicFields cwpf2 = new ClassWithPublicFields();

    @DeepImmutableField(value = "immutable reference and deep immutable field type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "Declared final Field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final FinalEmptyClass fec2 = new FinalEmptyClass();
}

@MutableType(value = "class has mutable fields n and name",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has mutable fields n and name",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class ClassWithPublicFields {

    @MutableField(value = "field is public",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public int n = 0;

    @MutableField(value = "field is public",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public String name = "name";
}


