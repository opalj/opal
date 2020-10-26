/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

@MutableType(value = "Class is extensible", analyses = {L0TypeImmutabilityAnalysis.class,
        L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "class has only the deep immutable field fec",
        analyses = {L1ClassImmutabilityAnalysis.class, L0ClassImmutabilityAnalysis.class})
public class ClassWithADeepImmutableFieldWhichIsSetInTheConstructor {

    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses  = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class})
    @DeepImmutableField(value = "immutable reference and deep immutable field type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "declared final field reference",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final FinalEmptyClass fec;

    public ClassWithADeepImmutableFieldWhichIsSetInTheConstructor(FinalEmptyClass fec) {
        this.fec = fec;
    }
}
