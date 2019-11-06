package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("")
public class private_getterEscape_deep {

    public FinalEmptyClass getFec() {
        return fec;
    }

    @ImmutableReferenceAnnotation("")
    @MutableFieldAnnotation("")
    private FinalEmptyClass fec = new FinalEmptyClass();
}
