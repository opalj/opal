package org.opalj.fpcf.fixtures.field_immutability;


import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

@MutableClassAnnotation("It has Mutable Fields")
public class TrivialMutableClass {

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is public")
    public int n = 0;

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is public")
    public String name = "name";
}
