package org.opalj.fpcf.fixtures.type_immutability;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("is has mutable fields")
@MutableClassAnnotation("It has Mutable Fields")
public class TrivialMutableClass {
    @MutableReferenceAnnotation("public")
    @MutableFieldAnnotation("mutable reference")
    public int n = 0;

    @MutableReferenceAnnotation("public")
    @MutableFieldAnnotation("mutable reference")
    public String name = "name";
}
