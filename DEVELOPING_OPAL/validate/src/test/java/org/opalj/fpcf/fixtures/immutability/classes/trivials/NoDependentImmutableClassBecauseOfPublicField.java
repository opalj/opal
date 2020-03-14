package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of mutable not final class")
@MutableClassAnnotation("Generic class but public field")
public class NoDependentImmutableClassBecauseOfPublicField<T> {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Field is public")
    public T t;
    NoDependentImmutableClassBecauseOfPublicField(T t){
        this.t = t;
    }
}
