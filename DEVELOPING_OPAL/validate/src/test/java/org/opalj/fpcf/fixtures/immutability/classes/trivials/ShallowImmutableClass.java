package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@DeepImmutableClassAnnotation("has shallow immutable field")
public class ShallowImmutableClass {
    @DeepImmutableFieldAnnotation("Because of mutable type")
    @ImmutableReferenceNotEscapesAnnotation("Because it is private")
    private MutableClass mutableClass = new MutableClass();
}
