package org.opalj.fpcf.fixtures.benchmark.arrays.transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@TransitivelyImmutableType("")
@TransitivelyImmutableClass("")
public class ArrayTransitiveImmutability {

    //@Immutable
    @TransitivelyImmutableField("The elements of the array can not escape")
    @NonAssignableFieldReference("Array is eager initialized")
    private Object[] array1 = new Object[]{1, 2, 3};

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private static Object[] staticDeepArray = new Object[5];

    @TransitivelyImmutableField("The elements of the array can not escape")
    @NonAssignableFieldReference("Array is initialized in the constructor")
    private Object[] a;

    public ArrayTransitiveImmutability() {
        a = new Object[]{5, 6, 7, 8};
    }

    //@Immutable
    @TransitivelyImmutableField("The elements of the array can not escape")
    @NonAssignableFieldReference("The array is not initialized.")
    private Object[] k;







    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private Object[] clonedArray = new Object[]{new Object(), new Object(), new Object()};

    public Object[] getClonedArray(){ return  clonedArray.clone(); }


}
