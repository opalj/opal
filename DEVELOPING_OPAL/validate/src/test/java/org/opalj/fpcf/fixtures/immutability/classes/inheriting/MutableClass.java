package org.opalj.fpcf.fixtures.immutability.classes.inheriting;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of mutable class")
@MutableClassAnnotation("Because of mutable field")
public class MutableClass {
    @MutableFieldAnnotation("Mutable reference")
    @MutableReferenceAnnotation("public field")
    public int n= 0;
}
