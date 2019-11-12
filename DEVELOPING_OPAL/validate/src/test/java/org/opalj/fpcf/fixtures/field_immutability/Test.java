package org.opalj.fpcf.fixtures.field_immutability;


import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

@ShallowImmutableClassAnnotation("It has only Shallow Immutable Fields")
public class Test {
    @ShallowImmutableFieldAnnotation("Because of Immutable Reference and Immutable Field Type")
    @ImmutableReferenceAnnotation("Effectively immutable")
    private String name = "name";
}
