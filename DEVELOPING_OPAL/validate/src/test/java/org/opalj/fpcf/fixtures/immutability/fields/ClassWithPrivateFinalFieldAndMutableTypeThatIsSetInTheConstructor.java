/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

@MutableType(value = "class is extensible", 
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@ShallowImmutableClass(value = "class has only the shallow immutable field tmc", 
analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
public class ClassWithPrivateFinalFieldAndMutableTypeThatIsSetInTheConstructor {

    @ShallowImmutableField(value = "immutable field reference and mutable type ClassWithPublicFields", 
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "declared final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final ClassWithPublicFields tmc;

    public ClassWithPrivateFinalFieldAndMutableTypeThatIsSetInTheConstructor(ClassWithPublicFields tmc) {
        this.tmc = tmc;
    }
}
