/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.string_elements;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.tac.fpcf.analyses.immutability.ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.L3FieldAssignabilityAnalysis;

/**
 * This class represents a simple model of the class java.util.String.
 * It encompasses cases of a shared array and a lazy initialized field storing a hash value.
 */
@TransitivelyImmutableType("Class is final")
@TransitivelyImmutableClass(value = "Class has only transitively immutable fields", analyses = {})
@NonTransitivelyImmutableClass(value = "the analysis only recognize non transitively immutable fields", analyses = {ClassImmutabilityAnalysis.class})
public final class SimpleStringModel {

    @TransitivelyImmutableField("The array values are not mutated anymore after the assignment ")
    @NonAssignableField("Field is final")
    private final char value[];

    public char[] getValue(){
        return value.clone();
    }

    @TransitivelyImmutableField("Lazy initialized field with primitive type")
    @LazilyInitializedField(value = "Field is lazily initialized", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis cannot reconizes determinism",
            analyses = {L3FieldAssignabilityAnalysis.class})
    private int hash; // Default value 0

    public SimpleStringModel(SimpleStringModel original) {
        this.value = original.value;
    }

    public SimpleStringModel(char[] value){
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
