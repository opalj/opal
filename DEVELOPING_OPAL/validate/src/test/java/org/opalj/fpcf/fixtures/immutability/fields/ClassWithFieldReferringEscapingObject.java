package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.fixtures.immutability.classes.inheriting.EmptyClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

@MutableType(value = "class is extensible",
        analyses =  {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@NonTransitivelyImmutableClass(value = "class has the shallow immutable field o",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
public class ClassWithFieldReferringEscapingObject {

    @NonTransitivelyImmutableField(value = "mutable referenced object can escape",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "final field",
            analyses = L3FieldAssignabilityAnalysis.class)
    private final Object o;

    public ClassWithFieldReferringEscapingObject() {
        this.o = new EmptyClass();
    }

    public ClassWithFieldReferringEscapingObject(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}