package org.opalj.fpcf.fixtures.cifi_benchmark.generic.extended;

import org.opalj.fpcf.fixtures.immutability.fields.FinalEmptyClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
//import edu.cmu.cs.glacier.qual.Immutable;

/**
 * This class represents the case when a generic class is extended and the generic type parameter concretized.
 */
//@Immutable
public class Specialization<T> {

    //@Immutable
    @DependentlyImmutableClass(value = "The field type is generic", parameter = {"A"})
    class G<A>{
        //@Immutable
        @DependentlyImmutableField(value="Has only a generic type", parameter = "A")
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    //@Immutable
    @TransitivelyImmutableClass("Generic Parameter specified with transitively immutable types")
    class G2 extends G<UseSiteVariance.FinalEmptyClass>{
        public G2(UseSiteVariance.FinalEmptyClass finalEmptyClass){
            super(finalEmptyClass);
        }
    }

    @TransitivelyImmutableField("The field is assigned with a concretized transitively immutable object.")
    private final Object o = new G<>(new FinalEmptyClass());
}


