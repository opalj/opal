package org.opalj.fpcf.fixtures.immutability.classes.generic;

//TODO

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public class GenericClassWithExtFinalMutTypes<A extends FinalMutableClass,B extends FinalMutableClass,C extends FinalMutableClass> {

    @ShallowImmutableFieldAnnotation("")
@ImmutableReferenceAnnotation("")
    private A a;

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private B b;

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private C c;
    GenericClassWithExtFinalMutTypes(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }

}

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
final class FinalMutableClass{
    public int n = 0;
}




