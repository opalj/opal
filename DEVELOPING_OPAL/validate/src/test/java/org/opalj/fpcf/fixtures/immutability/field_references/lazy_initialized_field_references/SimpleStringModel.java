/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

/**
 * This class represents a simple model of the string class.
 */
public class SimpleStringModel {

    @NonTransitivelyImmutableField(value= "field has immutable reference and array type char[]",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @EffectivelyNonAssignableField(value = "final field", analyses = L3FieldAssignabilityAnalysis.class)
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
