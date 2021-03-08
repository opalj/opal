/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("It has a mutable state")
@MutableClass("It has a mutable state")
public class ArrayWithOneEscapingObject {

    @MutableField("Reference of the field is mutable")
    @MutableFieldReference("Field is public")
    public Object o = new Object();

    @ShallowImmutableField("Field is initialized with an Shallow immutable field")
    @ImmutableFieldReference("Field is only initialized once.")
    private Object[] array2;

    public ArrayWithOneEscapingObject() {
        array2 = new Object[]{o};
    }

    @ShallowImmutableField("Field is initialized with a shallow immutable field.")
    @LazyInitializedThreadSafeFieldReference("Synchronized method with a guard-statement around the write")
    private Object[] array3;

    public synchronized void initArray3(Object o){
        if(array3==null)
            array3 = new Object[]{o};
    }

    @ShallowImmutableField("An array element escapes")
    @LazyInitializedThreadSafeFieldReference("Synchronized method, with guarding if-statement.")
    private Object[] array4;

    public synchronized Object initArray4(Object o){

        Object tmp0 = new Object();

        if(array4==null)
            array4 = new Object[]{tmp0};

        return tmp0;
    }
}
