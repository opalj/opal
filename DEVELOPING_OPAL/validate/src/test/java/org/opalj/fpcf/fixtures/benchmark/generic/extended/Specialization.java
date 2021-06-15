package org.opalj.fpcf.fixtures.benchmark.generic.extended;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class represents the case when a generic class is extended and the generic type parameter concretized.
 */
public class Specialization {

    @DependentlyImmutableClass(value = "", parameter = {"A"})
    class G<A>{
        @DependentlyImmutableField(value="Has only a generic type", parameter = "A")
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    @TransitivelyImmutableClass("Generic Parameter specified with transitively immutable types")
    class G2 extends G<finalEmptyClass>{
        public G2(finalEmptyClass finalEmptyClass){
            super(finalEmptyClass);
        }
    }

    @TransitivelyImmutableType("Final empty class")
    final class finalEmptyClass {}
}


