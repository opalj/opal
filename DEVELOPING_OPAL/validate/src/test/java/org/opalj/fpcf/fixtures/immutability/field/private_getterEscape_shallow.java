package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Because of not final class")
@ShallowImmutableClassAnnotation("Because it has only Shallow Immutable Fields")
public class private_getterEscape_shallow {
    public TrivialMutableClass getTmc() {
        return tmc;
    }

    @ShallowImmutableFieldAnnotation("Because of Immutable Reference and Mutable Field Type")
    @ImmutableReferenceEscapesAnnotation("effectively immutable")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
