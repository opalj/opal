package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@DeepImmutableClassAnnotation("Because it has only Deep Immutable Field Types")
public class privateFieldNotBlank_deep {
    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceAnnotation("Effectively Immutable Reference")
    private FinalEmptyClass name = new FinalEmptyClass();
}
