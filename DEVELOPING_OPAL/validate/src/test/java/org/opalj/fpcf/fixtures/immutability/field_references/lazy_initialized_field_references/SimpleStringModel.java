/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeButDeterministicReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

/**
 * This class represents a simple model of the string class.
 */
public class SimpleStringModel {

    @ShallowImmutableField(value= "field has immutable reference and array type char[]",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private final char value[];

    char[] getValue(){
        return value;
    }

    //TODO @DeepImmutableField
    //TODO @LazyInitializedNotThreadSafeButDeterministicReference
    @MutableField("Mutable field reference")
    @LazyInitializedNotThreadSafeFieldReference(value="The analysis state this for performance issues as being not thread safe") //The two def sites of h are conservatively handled as mutable")
    private int hash; // Default value 0


    public SimpleStringModel(SimpleStringModel original) {
        this.value = original.value;
    }

    public int hashCode() {
        int h = 0;
        if (hash == 0) {
            char val[] = value;
            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return hash;
    }
}
