package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@DeepImmutableClassAnnotation("Only Deep Immutable Fields")
public class private_getterEscape_deep {
    public FinalEmptyClass getFec() {
        return fec;
    }
    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceAnnotation("It is effectively immutable")
    private FinalEmptyClass fec = new FinalEmptyClass();
}