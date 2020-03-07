package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;

//TODO @DeepImmutableClassAnnotation("")
public class GenericExt2Deep<T extends EmptyClass> {
    //TODO @DeepImmutableFieldAnnotation("")
    private Generic_class1<T,T,T,T,T> gc;
    GenericExt2Deep(Generic_class1<T,T,T,T,T> gc) {
        this.gc = gc;
    }
}

final class EmptyClass{
}
