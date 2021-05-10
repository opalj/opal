package org.opalj.fpcf.fixtures.benchmark.generic.extended;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

public class Specialization {

    @DependentlyImmutableClass("")
    class G<A>{
        @DependentImmutableField(value="Has only a generic type", parameter = "A")
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    @TransitivelyImmutableClass("Generic Parameter specified with transitively immutable types")
    class G2 extends G<E>{
        public G2(E e){
            super(e);
        }
    }

    @TransitivelyImmutableType("Final empty class")
    final class E{}
}


