package org.opalj.fpcf.fixtures.immutability.classes;

import org.opalj.fpcf.properties.field_mutability.NonFinal;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeButDeterministicReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

class PossibleExceptionInInitialization {

    @LazyInitializedNotThreadSafeButDeterministicReferenceAnnotation("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        if (y == 0) {
            int z = 10 / i;
            y = x = 5;
        }
        return y;
    }
}

class CaughtExceptionInInitialization {

    @MutableReferenceAnnotation("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        try {
            if (y == 0) {
                int z = 10 / i;
                y = x = 5;
            }
            return y;
        } catch (Exception e) {
            return 0;
        }
    }
    //Test
    //com.sun.corba.se.impl.io.FVDCodeBaseImp
    //com.sun.xml.internal.bind.v2.model.impl.Messages
    //com.sun.xml.internal.bind.v2.model.impl.Messages
    //com.sun.jmx.defaults.JmxProperties
    //com.sun.jmx.defaults
    //com.sun.xml.internal.bind.v2.model.impl.ModelBuilder
    //javax.management.loading.MLet

    //com.sun.corba.se.impl.io.FVDCodeBaseImpl
    //
}