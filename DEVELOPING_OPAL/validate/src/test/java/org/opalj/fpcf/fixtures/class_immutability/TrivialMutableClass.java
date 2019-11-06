package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

@MutableClassAnnotation("because of public fields")
public class TrivialMutableClass {
    @MutableReferenceAnnotation("public")
    @MutableFieldAnnotation("mutable reference")
    public int n = 0;

    @MutableReferenceAnnotation("public")
    @MutableFieldAnnotation("mutable reference")
    public String name = "name";
}
