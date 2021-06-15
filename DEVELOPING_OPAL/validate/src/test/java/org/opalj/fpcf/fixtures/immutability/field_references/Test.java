package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;

public class Test {
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized method")
    private Integer synchronizedSimpleLazyInitializedIntegerField;

    public synchronized void initNO2(){
        if(synchronizedSimpleLazyInitializedIntegerField==0)
            synchronizedSimpleLazyInitializedIntegerField = 5;
    }
}
