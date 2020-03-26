package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.br.fpcf.properties.DependentImmutableField;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;

public class ArrayAndString<T> {
    @ShallowImmutableFieldAnnotation("")
    private String[] stringArray;
    @ShallowImmutableFieldAnnotation("")
    private int[] intArray;
    @DeepImmutableFieldAnnotation("")
    private String string;
    @DeepImmutableFieldAnnotation("")
    private int i;
    @ShallowImmutableFieldAnnotation("")
    private TrivialMutableClass[] tmc;
    @ShallowImmutableFieldAnnotation("")
    private T[] tArray;

    ArrayAndString(String[] stringArray, int[] intArray, String string, int i, TrivialMutableClass[] tmc, T[] tArray) {
        this.stringArray = stringArray;
        this.intArray = intArray;
        this.string = string;
        this.i = i;
        this.tmc = tmc;
        this.tArray = tArray;
    }

}
