package org.opalj.fpcf.fixtures.immutability.sandbox30;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

public class Test2<A extends FinalMutableClass> {

        @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
        @ShallowImmutableField(value = "immutable reference with a generic types that inherits a mutable type",
                analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
        @ImmutableFieldReference(value = "effective immutable field reference",
                analyses = L0FieldReferenceImmutabilityAnalysis.class)
        private A a;

        Test2(A a){
            this.a = a;
        }
    }

class FinalMutableClass{
    public int a = 5;
}
