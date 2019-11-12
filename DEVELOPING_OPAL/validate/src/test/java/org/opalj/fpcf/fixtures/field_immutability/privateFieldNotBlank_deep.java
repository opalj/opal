package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@DeepImmutableClassAnnotation("Because it has only Deep Immutable Field Types")
public class privateFieldNotBlank_deep {
    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceAnnotation("Effectively Immutable Reference")
    private FinalEmptyClass name = new FinalEmptyClass();
}
