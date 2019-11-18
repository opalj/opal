package org.opalj.fpcf.fixtures.type_immutability;

import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("has shallow mutable fields")
public class WithMutableAndImmutableFieldType {

    private FinalEmptyClass fec = new FinalEmptyClass();
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
