package org.opalj.fpcf.fixtures.immutability.open_world.generic.extended;

import org.opalj.fpcf.fixtures.immutability.open_world.general.FinalClassWithNoFields;
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
        @DependentlyImmutableField(value="Has only a generic type", parameter = {"A"})
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    @TransitivelyImmutableClass("The generic type parameter is specialized as transitively immutable")
    @DependentlyImmutableClass(value = "The analysis is not aware of the specialization", parameter = {"A"})
    class G2 extends G<FinalClassWithNoFields>{
        public G2(FinalClassWithNoFields finalClassWithNoFields){
            super(finalClassWithNoFields);
        }
    }
}
