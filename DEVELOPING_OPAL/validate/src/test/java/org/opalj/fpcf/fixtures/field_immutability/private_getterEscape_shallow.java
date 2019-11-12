package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("Becaus it has only Shallow Immutable Fields")
public class private_getterEscape_shallow {
    public TrivialMutableClass getTmc() {
        return tmc;
    }

    @ShallowImmutableFieldAnnotation("Because of Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("effectively immutable")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
