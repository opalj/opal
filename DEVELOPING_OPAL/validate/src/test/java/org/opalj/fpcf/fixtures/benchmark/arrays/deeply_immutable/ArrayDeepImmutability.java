package org.opalj.fpcf.fixtures.benchmark.arrays.deeply_immutable;

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

    //@Immutable
    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is eager initialized")
    private Object[] array1 = new Object[]{1, 2, 3};

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private static Object[] staticDeepArray = new Object[5];

    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is initialized in the constructor")
    private Object[] a;

    public ArrayDeepImmutability() {
        a = new Object[]{5, 6, 7, 8};
    }

    //@Immutable
    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("The array is not initialized.")
    private Object[] k;







    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Object[] clonedArray = new Object[]{new Object(), new Object(), new Object()};

    public Object[] getClonedArray(){ return  clonedArray.clone(); }


}
