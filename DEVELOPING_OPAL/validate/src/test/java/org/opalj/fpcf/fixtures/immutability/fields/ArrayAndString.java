/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;

public class ArrayAndString<T> {

    @NonTransitivelyImmutableField("")
    private String[] stringArray;

    @NonTransitivelyImmutableField("")
    private int[] intArray;

    @TransitivelyImmutableField("")
    private String string;

    @TransitivelyImmutableField("")
    private int i;

    @NonTransitivelyImmutableField("")
    private ClassWithPublicFields[] tmc;

    @NonTransitivelyImmutableField("")
    private T[] tArray;

    ArrayAndString(String[] stringArray, int[] intArray, String string, int i,
                   ClassWithPublicFields[] tmc, T[] tArray) {
        this.stringArray = stringArray;
        this.intArray = intArray;
        this.string = string;
        this.i = i;
        this.tmc = tmc;
        this.tArray = tArray;
    }

}
