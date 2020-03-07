package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DependentImmutableClassAnnotation("Das dependent immutable field")
public class DependentImmutableClass<T> {
    private T t;
    public DependentImmutableClass(T t){
        this.t = t;
    }
}
