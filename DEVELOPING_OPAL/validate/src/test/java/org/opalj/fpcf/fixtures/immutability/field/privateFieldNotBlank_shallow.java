package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@DeepImmutableClassAnnotation("Because it has only Shallow Immutable Fields")
public class privateFieldNotBlank_shallow {

    @DeepImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("Effectively Immutable Reference")
    private TrivialMutableClass tmc = new TrivialMutableClass();
}
