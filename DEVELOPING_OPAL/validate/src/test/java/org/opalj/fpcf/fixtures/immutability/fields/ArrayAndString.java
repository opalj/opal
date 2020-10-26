/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

public class ArrayAndString<T> {

    @ShallowImmutableField("")
    private String[] stringArray;

    @ShallowImmutableField("")
    private int[] intArray;

    @DeepImmutableField("")
    private String string;

    @DeepImmutableField("")
    private int i;

    @ShallowImmutableField("")
    private ClassWithPublicFields[] tmc;

    @ShallowImmutableField("")
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
