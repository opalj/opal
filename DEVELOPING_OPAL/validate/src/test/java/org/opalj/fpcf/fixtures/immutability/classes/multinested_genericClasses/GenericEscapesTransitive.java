package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;


@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
final class ClassWithGenericField_shallow {
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass<SimpleMutableClass, FinalEmptyClass,FinalEmptyClass> gc =
            new SimpleGenericClass<SimpleMutableClass,FinalEmptyClass,FinalEmptyClass>
                    (new SimpleMutableClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

class FinalEmptyClass{}
class SimpleMutableClass{ public int n = 10;}

class SimpleGenericClass<A,B,C> {
    A a;
    B b;
    C c;
    SimpleGenericClass(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

