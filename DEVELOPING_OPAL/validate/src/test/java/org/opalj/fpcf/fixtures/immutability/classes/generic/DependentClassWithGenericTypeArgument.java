package org.opalj.fpcf.fixtures.immutability.classes.generic;


import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public class DependentClassWithGenericTypeArgument<T> {
    private final Generic_class1<TrivialMutableClass,T,T,T,T> gc1;
    DependentClassWithGenericTypeArgument(Generic_class1<TrivialMutableClass,T,T,T,T> gc1) {
        this.gc1 = gc1;
    }
}
