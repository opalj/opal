package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;


public class protectedClass_deep {
    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is declared as protected")
    protected FinalEmptyClass fec1 = new FinalEmptyClass();

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is declared as protected")
    protected TrivialMutableClass tmc1 = new TrivialMutableClass();

    @DeepImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceAnnotation("Declared final Field")
    private final TrivialMutableClass tmc2 = new TrivialMutableClass();

    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceAnnotation("Declared final Field")
    private final FinalEmptyClass fec2 = new FinalEmptyClass();
}





@MutableClassAnnotation("It has Mutable Fields")
class TrivialMutableClass {

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is public")
    public int n = 0;

    @MutableFieldAnnotation("Because of Mutable Reference")
    @MutableReferenceAnnotation("Because it is public")
    public String name = "name";
}


