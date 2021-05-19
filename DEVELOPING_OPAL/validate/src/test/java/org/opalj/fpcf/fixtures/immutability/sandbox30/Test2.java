package org.opalj.fpcf.fixtures.immutability.sandbox30;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

public class Test2<A extends FinalMutableClass> {

        @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
        @NonTransitivelyImmutableField(value = "immutable reference with a generic types that inherits a mutable type",
                analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
        @EffectivelyNonAssignableField(value = "effective immutable field reference",
                analyses = L3FieldAssignabilityAnalysis.class)
        private A a;

        Test2(A a){
            this.a = a;
        }
    }

class FinalMutableClass{
    public int a = 5;
}
