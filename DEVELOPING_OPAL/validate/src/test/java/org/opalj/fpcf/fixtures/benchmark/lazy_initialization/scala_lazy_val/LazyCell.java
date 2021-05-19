package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.scala_lazy_val;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 *  This class represents the implementation of Scala lazy val from Scala 2.12.
 *  https://docs.scala-lang.org/sips/improved-lazy-val-initialization.html
 *
 */
@MutableType("non final class")
@TransitivelyImmutableClass("Class has only transitive immutable fields.")
public class LazyCell {

    @TransitivelyImmutableField("Lazy initialized field with primitive type")
    @LazyInitializedThreadSafeFieldReference("The field is only set once in a synchronized way.")
    private volatile boolean bitmap_0 = false;

    @TransitivelyImmutableField("Lazy initialized field with primitive type")
    @LazyInitializedThreadSafeFieldReference("The field is only set once in a synchronized way.")
    Integer value_0;

    private Integer value_lzycompute() {
            synchronized (this){
                if(value_0==0) {
                    value_0 = 42;
                    bitmap_0 = true;
                }
            }
            return value_0;
    }

    public Integer getValue(){
        return bitmap_0 ? value_0 : value_lzycompute();
    }
}
