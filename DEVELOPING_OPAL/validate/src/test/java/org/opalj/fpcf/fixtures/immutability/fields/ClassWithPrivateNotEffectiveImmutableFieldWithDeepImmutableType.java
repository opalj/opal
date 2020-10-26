/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass("Because it has Mutable Fields")
public class ClassWithPrivateNotEffectiveImmutableFieldWithDeepImmutableType {

    public void setFec(FinalEmptyClass fec) {
        this.fec = fec;
    }

    @MutableField("Because of Mutable Reference")
    @MutableFieldReference("Not final field could be set via setter")
    private FinalEmptyClass fec = new FinalEmptyClass();
}
