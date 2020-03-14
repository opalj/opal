package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@DependentImmutableClassAnnotation("Das dependent immutable field")
public class DependentImmutableClass<T> {
    @DependentImmutableFieldAnnotation(value = "Because of type T",genericString = "T")
    @ImmutableReferenceAnnotation("Private effectively final field")
    private T t;
    public DependentImmutableClass(T t){
        this.t = t;
    }
}
