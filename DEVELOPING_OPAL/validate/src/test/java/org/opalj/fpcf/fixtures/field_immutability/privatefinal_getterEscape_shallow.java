package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("It has only Shallow Immutable Fields")
public class privatefinal_getterEscape_shallow {
    public TrivialMutableClass getTmc() {
        return tmc;
    }

    @ShallowImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("Declared final Reference")
    private final TrivialMutableClass tmc = new TrivialMutableClass();
}
