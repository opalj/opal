package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class ArrayInitialization {
    @MutableReferenceAnnotation("")
    private Object[] array;

    public Object[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new Object[n];
        }
        return array;
    }
    //@MutableReferenceAnnotation("")
    private Object[] b;
    public Object[] getB(boolean flag) throws Exception{
        if(b!=null)
            return b;
        else if(flag)
            return null; //throw new Exception("");
        else {
            this.b = new Object[5];
            return b;
        }

    }
}
class SimpleLazyObjectsInstantiation{
    @LazyInitializedNotThreadSafeReferenceAnnotation("")
    private SimpleLazyObjectsInstantiation instance;
    public SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}




class EscapingObjectDeep {
    @DeepImmutableFieldAnnotation("")
    private Object o;
    public synchronized Object getO(){
        if(this.o==null)
            this.o = new Object();
        return this.o;
    }
}

class EscapingObjectShallow {
    @ShallowImmutableFieldAnnotation("")
    private final Object o;
    public EscapingObjectShallow() {
        this.o = new EmptyClass();
    }
    public EscapingObjectShallow(int n) {
        this.o = new Object();
    }
    public Object getO(){
        return this.o;
    }
}

class ClassUsingEmptyClass {

    @DeepImmutableFieldAnnotation("")
    private EmptyClass emptyClass = new EmptyClass();

    public EmptyClass getEmptyClass() {
        return this.emptyClass;
    }

}
class ClassUsingEmptyClassExtensible {

    @ShallowImmutableFieldAnnotation("")
    private EmptyClass emptyClass = new EmptyClass();

    public ClassUsingEmptyClassExtensible(EmptyClass emptyClass) {
        this.emptyClass = emptyClass;
    }

}




class EmptyClass {}