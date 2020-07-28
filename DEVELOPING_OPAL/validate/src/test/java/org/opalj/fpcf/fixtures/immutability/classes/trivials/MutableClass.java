package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableType("Because of not final mutable class")
@MutableClassAnnotation("Because of mutable field")
public class MutableClass {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Because of public field")
    public int n = 0;
}