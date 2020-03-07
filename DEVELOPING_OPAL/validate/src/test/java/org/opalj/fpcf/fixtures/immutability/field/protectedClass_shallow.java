package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;

@MutableClassAnnotation("It has Mutable Fields")
public class protectedClass_shallow {
    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is declared as protected")
    protected TrivialMutableClass tmc = new TrivialMutableClass();
}
