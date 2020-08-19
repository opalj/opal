package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class ArrayWithOneEscapingObject {
 @MutableFieldAnnotation("Reference of the field is mutable")
    @MutableReferenceAnnotation("Field is public")
    public Object o = new Object();

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("Reference is only initialized once")
    private Object[] array1 = new Object[]{o, new Object(), new Object()}; //TODO


    @ShallowImmutableFieldAnnotation("Field is initialized with an Shallow immutable field")
    @ImmutableReferenceAnnotation("Field is only initialized once.")
    private Object[] array2;

    public ArrayWithOneEscapingObject() {
        array2 = new Object[]{o};
    }

    @ShallowImmutableFieldAnnotation("Field is initialized with a shallow immutable field.")
    @LazyInitializedThreadSafeReferenceAnnotation("Synchronized method with a guard-statement around the write")
    private Object[] array3;

    public synchronized void initArray3(Object o){
        if(array3==null)
            array3 = new Object[]{o};
    }

    @ShallowImmutableFieldAnnotation("An array element escapes")
    @LazyInitializedThreadSafeReferenceAnnotation("Synchronized method, with guarding if-statement.")
    private Object[] array4;

    public synchronized Object initArray4(Object o){
        Object tmp0 = new Object();
        if(array4==null)
            array4 = new Object[]{tmp0};
        return tmp0;
    }
}
