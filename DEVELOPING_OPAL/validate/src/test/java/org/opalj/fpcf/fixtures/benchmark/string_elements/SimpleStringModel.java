/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.string_elements;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
//import edu.cmu.cs.glacier.qual.Immutable;

/**
 * This class represents a simple model of the class java.util.String.
 * It encompasses cases of a shared array and a lazy initialized field storing a hash value.
 */
//@Immutable
@TransitivelyImmutableType("Class is final")
@TransitivelyImmutableClass("Class has only transitively immutable fields")
public final class SimpleStringModel {

    //@Immutable
    @TransitivelyImmutableField("The array values are not mutated anymore after the assignment ")
    @NonAssignableField("Field is final")
    private final char value[];

    public char[] getValue(){
        return value.clone();
    }

    //@Immutable
    @TransitivelyImmutableField("Lazy initialized field with primitive type")
    @LazilyInitializedField("Field is lazily initialized")
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
