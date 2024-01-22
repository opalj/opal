/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.extended;

import org.opalj.fpcf.fixtures.immutability.openworld.general.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.immutability.openworld.general.FinalClassWithNoFields;
import org.opalj.fpcf.fixtures.immutability.openworld.generic.simple.Generic;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;

/**
 * Class represents different cases of nested genericity.
 */
public class NestedGenericFields<T> {

    @TransitivelyImmutableField(value = "The generic types are nested transitively immutable", analyses = {})
    @NonTransitivelyImmutableField("The analysis is not able to recognize nested genericity")
    @NonAssignableField("field is final")
    private final Generic<Generic<FinalClassWithNoFields>> nestedTransitivelyImmutable =
            new Generic<>(new Generic<>(new FinalClassWithNoFields()));

    @DependentlyImmutableField(value = "The immutability of the field depends on the generic type parameter T",
            parameter = {"T"}, analyses = {})
    @NonTransitivelyImmutableField("The analysis is not able to recognize nested genericity")
    @NonAssignableField("field is final")
    private final Generic<Generic<T>> nestedAssignable;

    @DependentlyImmutableField(value = "The immutability of the field depends on the generic type parameter T",
            parameter={"T"}, analyses = {})
    @NonTransitivelyImmutableField("The analysis is not able to recognize nested genericity")
    @NonAssignableField("field is final")
    private final Generic<Generic<T>> nestedDependent;

    @NonTransitivelyImmutableField("Only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final Generic<Generic<ClassWithMutableFields>> nestedNonTransitive =
            new Generic<>(new Generic<>(new ClassWithMutableFields()));

    public NestedGenericFields(T t){
        this.nestedDependent = new Generic<>(new Generic<>(t));
        this.nestedAssignable = new Generic<>(new Generic<>(t));
    }
}
