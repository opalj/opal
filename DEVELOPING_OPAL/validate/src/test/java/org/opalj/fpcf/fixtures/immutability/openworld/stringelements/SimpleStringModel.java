/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.stringelements;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.tac.fpcf.analyses.FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * This class represents a simple model of the class java.util.String.
 * It encompasses cases of a shared array and a lazy initialized field storing a hash value.
 */
public final class SimpleStringModel {

    @TransitivelyImmutableField(value = "The array values are not mutated after the assignment ", analyses = {})
    @MutableField(value = "The analysis can not recognize transitive immutable arrays, and the field is assignable",
            analyses = { FieldImmutabilityAnalysis.class })
    @AssignableField(value = "The field is read/written in two different initializers and the analysis does not consider whether they call each other")
    private final char value[];

    public char[] getValue() {
        return value.clone();
    }

    @TransitivelyImmutableField(value = "Lazy initialized field with primitive type", analyses = {})
    @LazilyInitializedField(value = "Field is lazily initialized", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis cannot recognize determinism",
            analyses = { L2FieldAssignabilityAnalysis.class })
    private int hash; // Default value 0

    public SimpleStringModel(SimpleStringModel original) {
        this.value = original.value;
    }

    public SimpleStringModel(char[] value) {
        this.value = value.clone();
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
