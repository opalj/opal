package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
public class TrivialMutableClass {

    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public int n = 0;

    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public String name = "name";
}
