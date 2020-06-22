package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public class Two<A,B> {

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<Generic_class1<A, A, A, A, A>, B, B, B, TrivialMutableClass> gc1;

    public Two(A a, B b, TrivialMutableClass tmc, Generic_class1 gc1) {
         this.gc1 = new Generic_class1<Generic_class1<A, A, A, A, A>, B, B, B, TrivialMutableClass>(gc1,b,b,b,tmc);

    }


}
