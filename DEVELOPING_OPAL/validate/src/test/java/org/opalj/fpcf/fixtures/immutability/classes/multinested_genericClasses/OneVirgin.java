package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
public class OneVirgin<A,B,C,D, E> {
    @DependentImmutableFieldAnnotation(value="",genericString = "A")
    @ImmutableReferenceEscapesAnnotation("")
    private A a;
    @DependentImmutableFieldAnnotation(value="",genericString = "B")
    @ImmutableReferenceEscapesAnnotation("")
    private B b;
    @DependentImmutableFieldAnnotation(value="",genericString = "C")
    @ImmutableReferenceEscapesAnnotation("")
    private C c;
    @DependentImmutableFieldAnnotation(value="",genericString = "D")
    @ImmutableReferenceEscapesAnnotation("")
    private D d;

    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public TrivialMutableClass tmc;

    Generic_class1<A,B,C, D, TrivialMutableClass> gc1;
    public OneVirgin(A a, B b, C c, D  d, TrivialMutableClass e){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.tmc = tmc;
        this.gc1 = new Generic_class1<A,B,C, D, TrivialMutableClass>(this.a, this.b, this.c, this.d, this.tmc);
    }


}
