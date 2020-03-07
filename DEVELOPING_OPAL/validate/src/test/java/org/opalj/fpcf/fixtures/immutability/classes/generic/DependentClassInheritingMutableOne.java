package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
public class DependentClassInheritingMutableOne<T> extends TrivialMutableClass {
    private final T field;
    public DependentClassInheritingMutableOne(T field) {
        this.field = field;
    }
}
