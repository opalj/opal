package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.scala_lazy_val;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;

public class LazyCell {

    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("Scala lazy val pattern")
    private volatile boolean bitmap_0 = false;

    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("Scala lazy val pattern")
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
