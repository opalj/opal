package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;

//TODO @ShallowImmutableClassAnnotation("")
public class GenericExt2Shallow<T extends FinalMutableClass> {
    //TODO @ShallowImmutableFieldAnnotation("")
    private Generic_class1<T,T,T,T,T> gc;
    GenericExt2Shallow(Generic_class1<T,T,T,T,T> gc) {
        this.gc = gc;
    }
}

final class FinalMutableClass{
    public int n = 0;
}