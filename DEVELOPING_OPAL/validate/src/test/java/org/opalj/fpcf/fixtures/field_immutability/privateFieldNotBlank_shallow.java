package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("Because it has only Shallow Immutable Fields")
public class privateFieldNotBlank_shallow {

    @ShallowImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("Effectively Immutable Reference")
    private TrivialMutableClass tmc = new TrivialMutableClass();
}
