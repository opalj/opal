/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

//TODO @ShallowImmutableClassAnnotation("")
public class GenericExt2Shallow<T extends FinalMutableClass> {
   /* //TODO @ShallowImmutableFieldAnnotation("")
    private GenericClass<T,T,T,T,T> gc;
    GenericExt2Shallow(GenericClass<T,T,T,T,T> gc) {
        this.gc = gc;
    }*/
}

final class FinalMutableClass{
    public int n = 0;
}