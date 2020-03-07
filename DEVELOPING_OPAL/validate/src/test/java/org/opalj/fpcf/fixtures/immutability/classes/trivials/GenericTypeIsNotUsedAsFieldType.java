package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("Generic Type is not used as field Type")
public class GenericTypeIsNotUsedAsFieldType<T> {
    private int n = 0;
    GenericTypeIsNotUsedAsFieldType(T t){
        String s = t.toString();
    }
}
