package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@DeepImmutableTypeAnnotation("has shallow and mutable fields")
@DeepImmutableClassAnnotation("has shallow and immutable fields")
public final class WithMutableAndImmutableFieldType {

    @DeepImmutableFieldAnnotation("immutable reference and deep immutable type")
    @ImmutableReferenceAnnotation("private field")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @DeepImmutableFieldAnnotation("imm reference and mutable type")
    @ImmutableReferenceAnnotation("private field")
    private SimpleMutableClass tmc = new SimpleMutableClass();
}

@MutableType("Class has no fields but is not final")
@DeepImmutableClassAnnotation("Class has no fields")
class EmptyClass {
}

@DeepImmutableClassAnnotation("Class has no fields and is final")
@DeepImmutableTypeAnnotation("Class has no fields")
final class FinalEmptyClass {
}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
class SimpleMutableClass{ public int n=0;}
