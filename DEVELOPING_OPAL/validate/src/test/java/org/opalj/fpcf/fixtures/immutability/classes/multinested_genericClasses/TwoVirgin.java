package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

public class TwoVirgin<A,B, C, D, E> {
    @DependentImmutableFieldAnnotation(value="",genericString = "")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, C, D, E> gc1;

    public TwoVirgin(A a, B b, C c, Generic_class1 gc1) {
         gc1 = new Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, B, B, C>(gc1,b,b,b,c);
    }


}
