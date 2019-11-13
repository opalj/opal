package org.opalj.fpcf.fixtures.type_immutability;

import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@ShallowImmutableTypeAnnotation("has shallow mutable fields")
public class WithMutableAndImmutableFieldType {

    private FinalEmptyClass fec = new FinalEmptyClass();

    private TrivialMutableClass tmc = new TrivialMutableClass();

}
