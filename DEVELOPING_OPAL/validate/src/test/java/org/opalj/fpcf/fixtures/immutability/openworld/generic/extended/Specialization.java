/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.extended;

import org.opalj.fpcf.fixtures.immutability.openworld.general.FinalClassWithNoFields;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;

/**
 * This file represents the case when a generic class is extended and the generic type parameter concretized.
 */

    @DependentlyImmutableClass(value = "The field type is generic", parameter = {"A"})
    class G<A>{
        @DependentlyImmutableField(value="Has only a generic type", parameter = {"A"})
        private final A a;
        public G(A a){
            this.a = a;
        }
    }

    @TransitivelyImmutableClass(value = "The generic type parameter is specialized as transitively immutable",
            analyses = {})
    @DependentlyImmutableClass(value = "The analysis is not aware of the specialization", parameter = {"A"})
    class G2 extends G<FinalClassWithNoFields>{
        public G2(FinalClassWithNoFields finalClassWithNoFields){
            super(finalClassWithNoFields);
        }
    }
