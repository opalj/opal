package org.opalj.fpcf.fixtures.immutability.generic.extended;

import org.opalj.fpcf.fixtures.immutability.fields.FinalEmptyClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;

/**
 * This class represents the case when a generic class is extended and the generic type parameter concretized.
 */
public class Specialization<T> {

    @DependentlyImmutableClass(value = "The field type is generic", parameter = {"A"})
    class G<A>{
        @DependentlyImmutableField(value="Has only a generic type", parameter = "A")
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    @TransitivelyImmutableClass("Generic Parameter specified with transitively immutable types")
    class G2 extends G<UseSiteVariance.FinalEmptyClass>{
        public G2(UseSiteVariance.FinalEmptyClass finalEmptyClass){
            super(finalEmptyClass);
        }
    }

    @TransitivelyImmutableField("The field is assigned with a concretized transitively immutable object.")
    private final Object o = new G<>(new FinalEmptyClass());
}
