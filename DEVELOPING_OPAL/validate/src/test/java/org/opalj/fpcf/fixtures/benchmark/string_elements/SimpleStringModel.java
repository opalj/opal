/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.string_elements;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class represents a simple model of the string class.
 * This includes cases of a shared array and a lazy initialized field storing a hash value.
 */
@TransitivelyImmutableType("Final class with only non-assignable fields")
@TransitivelyImmutableClass("Final class with only not assignable fields")
public final class SimpleStringModel {

    @TransitivelyImmutableField("The array values are after the assignment no more mutated")
    @NonAssignableFieldReference("final field")
    private final char value[];

    public char[] getValue(){
        return value.clone();
    }

    @TransitivelyImmutableField("Lazy initialized field with primitive type")
    @LazyInitializedThreadSafeFieldReference("")
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
