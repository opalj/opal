/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

public class ClassWithProtectedFields {
    @MutableField(value = "the field has a mutable field reference",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "the field is protected",
            analyses = L3FieldAssignabilityAnalysis.class)
    protected FinalEmptyClass fec1 = new FinalEmptyClass();

    @MutableField("Because of Mutable Reference")
    @AssignableField(value = "Because it is declared as protected",
            analyses = L3FieldAssignabilityAnalysis.class)
    protected ClassWithPublicFields cwpf1 = new ClassWithPublicFields();

    @NonTransitivelyImmutableField(value = "field has an immutable reference and mutable type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "Declared final Field",
            analyses = L3FieldAssignabilityAnalysis.class)
    private final ClassWithPublicFields cwpf2 = new ClassWithPublicFields();

    @TransitivelyImmutableField(value = "immutable reference and deep immutable field type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "Declared final Field",
            analyses = L3FieldAssignabilityAnalysis.class)
    private final FinalEmptyClass fec2 = new FinalEmptyClass();
}

@MutableType(value = "class has mutable fields n and name",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has mutable fields n and name",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class ClassWithPublicFields {

    @MutableField(value = "field is public",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public",
            analyses = L3FieldAssignabilityAnalysis.class)
    public int n = 0;

    @MutableField(value = "field is public",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public",
            analyses = L3FieldAssignabilityAnalysis.class)
    public String name = "name";
}


