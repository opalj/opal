/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

//TODO @DeepImmutableClassAnnotation("")
public class GenericExt2Deep<T extends EmptyClass> {
  /*  //TODO @DeepImmutableFieldAnnotation("")
    private GenericClass<T,T,T,T,T> gc;
    GenericExt2Deep(GenericClass<T,T,T,T,T> gc) {
        this.gc = gc;
    }*/
}

final class EmptyClass{
}
