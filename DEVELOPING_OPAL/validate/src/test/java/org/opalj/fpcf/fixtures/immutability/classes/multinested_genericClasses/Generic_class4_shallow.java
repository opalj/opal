package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@ShallowImmutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public final class Generic_class4_shallow {

    @ImmutableReferenceEscapesAnnotation("")
    @ShallowImmutableFieldAnnotation("")
    private Generic_class3<TrivialMutableClass> gc;

    public Generic_class4_shallow(TrivialMutableClass tmc1, FinalEmptyClass fec2, FinalEmptyClass fec3, FinalEmptyClass fec4, FinalEmptyClass fec5){
        gc = new Generic_class3<TrivialMutableClass>(tmc1, fec2, fec3, fec4, fec5);
    }
}