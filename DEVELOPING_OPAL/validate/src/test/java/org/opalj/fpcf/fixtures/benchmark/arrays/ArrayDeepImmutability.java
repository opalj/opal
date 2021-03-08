package org.opalj.fpcf.fixtures.benchmark.arrays;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;

//@Immutable
@DeepImmutableType("")
@DeepImmutableClass("")
public class ArrayDeepImmutability {

    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is eager initialized")
    private Object[] zzz = new Object[]{1, 2, 3};

    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is initialized in the constructor")
    private Object[] a;

    public ArrayDeepImmutability() {
        a = new Object[]{5, 6, 7, 8};
    }

    @DeepImmutableField("The elements of the array can not escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized thread safen and eagerly.")
    private Object[] j;

    public synchronized void getJ(){
        if(j==null)
            j = new Object[]{new Object(), new Object(), new Object()};
    }


    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("The array is not initialized.")
    private Object[] k;


    @DeepImmutableField("The elements of the array can not escape.")
    @ImmutableFieldReference("The array is initialized eagerly.")
    private Object[] h = new Object[]{new Object(), new Object(), new Object()};


    @DeepImmutableField("The elements of the array can escape, but have a deep immutable reference.")
    @LazyInitializedThreadSafeFieldReference("The array is thread safe lazily intialized.")
    private Integer[] q;
    public synchronized Integer getQ(){
        if(q==null)
            q = new Integer[]{new Integer(1), new Integer(2), new Integer(3)};
        return q[2];
    }

    @DeepImmutableField("The elements of the array can escape.")
    @LazyInitializedThreadSafeFieldReference("The array is thread-safely lazily initialized")
    private Object[] arr;

    public synchronized Object[] getI(){
        if(arr==null)
            arr = new Object[]{new Object(), new Object(), new Object()};
        return arr;
    }

    @DeepImmutableField("")
    @ImmutableFieldReference("Reference is only initialized once")
    private Object[] array1 = new Object[]{new Object(), new Object(), new Object()}; //TODO


    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Object[] clonedArray = new Object[]{new Object(), new Object(), new Object()};

    public Object[] getClonedArray(){ return  clonedArray.clone(); }
}
