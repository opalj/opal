package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;

@DeepImmutableClassAnnotation("It has only Deep Immutable Fields")
public class privatefinal_getterEscape_deep {
    public FinalEmptyClass getFec() {
        return fec;
    }

    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceEscapesAnnotation("Declared final Field")
    private final FinalEmptyClass fec = new FinalEmptyClass();

}
