package org.opalj.fpcf.fixtures.cifi_benchmark.sandbox;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;

    public class Test2 {
        @NonTransitivelyImmutableField("")
        public final M m = new M();

        @NonTransitivelyImmutableField("")
        public final M m2;
        public Test2(M m){
            this.m2 = m;
        }
    }

    class M {
        public int n = 8;
     /*   com.sun.xml.internal.ws.util.QNameMap

        com.sun.org.glassfish.external.amx.MBeanListener.mCallback

        com.sun.xml.internal.bind.v2.model.impl.ReferencePropertyInfoImpl.domHandler

        sun.security.ec.point.ProjectivePoint.x*/
    }


