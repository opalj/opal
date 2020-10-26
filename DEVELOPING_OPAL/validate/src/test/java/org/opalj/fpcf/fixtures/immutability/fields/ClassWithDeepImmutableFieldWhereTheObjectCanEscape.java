/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@DeepImmutableClass(value = "class has only the deep immutable field fec",
analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
public class ClassWithDeepImmutableFieldWhereTheObjectCanEscape {

    @DeepImmutableField("immutable reference and immutable field type FinalEmptyClass")
    @ImmutableFieldReference("field is effective immutable")
    private FinalEmptyClass fec = new FinalEmptyClass();

    public FinalEmptyClass getFec() {
        return fec;
    }
}