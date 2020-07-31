package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("It has only Shallow Immutable Fields")
public class privateFinalFieldBlank_costructorEscape_shallow {

    @ShallowImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("Declared final Field")
    private final TrivialMutableClass tmc;

    public privateFinalFieldBlank_costructorEscape_shallow(TrivialMutableClass tmc) {
        this.tmc = tmc;
    }
}
