package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public class GenericClassWithDeepImmParams<A extends FinalEmptyClass,B extends FinalEmptyClass,C extends FinalEmptyClass> {
    @DeepImmutableFieldAnnotation("")
    private A a;
    @DeepImmutableFieldAnnotation("")
    private B b;
    @DeepImmutableFieldAnnotation("")
    private C c;
    GenericClassWithDeepImmParams(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
