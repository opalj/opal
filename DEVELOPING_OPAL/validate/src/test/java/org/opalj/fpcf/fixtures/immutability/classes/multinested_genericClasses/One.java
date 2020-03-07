package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
public class One<A,B,C,D> {
    @DependentImmutableFieldAnnotation(value="",genericString = "A")
    @ImmutableReferenceAnnotation("")
    private A a;
    @DependentImmutableFieldAnnotation(value="",genericString = "B")
    @ImmutableReferenceAnnotation("")
    private B b;
    @DependentImmutableFieldAnnotation(value="",genericString = "C")
    @ImmutableReferenceAnnotation("")
    private C c;
    @DependentImmutableFieldAnnotation(value="",genericString = "D")
    @ImmutableReferenceAnnotation("")
    private D d;
    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    TrivialMutableClass tmc;
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<A,B,C, D, TrivialMutableClass> gc1;
    public One(A a, B b, C c, D  d, TrivialMutableClass tmc){
        gc1 = new Generic_class1<A,B,C, D, TrivialMutableClass>(a,b,c,d, tmc);
    }


}
