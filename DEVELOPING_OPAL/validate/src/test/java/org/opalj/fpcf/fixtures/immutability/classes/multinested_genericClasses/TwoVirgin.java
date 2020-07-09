package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;

public class TwoVirgin<A,B, C, D, E> {
    @DependentImmutableFieldAnnotation(value="",genericString = "")
    @ImmutableReferenceEscapesAnnotation("")
    private Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, C, C, C> gc1;

    public TwoVirgin(A a, B b, C c, Generic_class1<Generic_class1, A, A, A, A> gc1) {
         this.gc1 = new Generic_class1<Generic_class1<Generic_class1, A, A, A, A>, B, C, C, C>(gc1,b,c,c,c);
    }


}
