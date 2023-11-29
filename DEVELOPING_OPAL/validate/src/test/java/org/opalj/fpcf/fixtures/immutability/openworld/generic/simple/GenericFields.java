/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.simple;

import org.opalj.fpcf.fixtures.immutability.openworld.general.ClassWithMutableFields;
import org.opalj.fpcf.fixtures.immutability.openworld.general.FinalClassWithNoFields;
import org.opalj.fpcf.fixtures.immutability.openworld.general.FinalClassWithNonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class with multiple possibilities of generic types in combination with immutability.
 */
@MutableType("The class is extensible")
@NonTransitivelyImmutableClass("The class has non transitively immutable fields")
public class GenericFields<T> {

    @NonTransitivelyImmutableField("The generic type parameter is concreted with a mutable type")
    @NonAssignableField("field is final")
    private final Generic<ClassWithMutableFields> singleMutable;

    @TransitivelyImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final Generic<FinalClassWithNoFields> singleTransitive;

    @NonTransitivelyImmutableField("Two generic type parameters are concreted with a mutable type")
    @NonAssignableField("field is final")
    private final MultipleGeneric<T,ClassWithMutableFields,FinalClassWithNoFields> multipleMutable;

    @NonTransitivelyImmutableField("There is only a non transitively immutable type parameter")
    @NonAssignableField("field is final")
    private final Generic<FinalClassWithNonTransitivelyImmutableField> singleNonTransitivelyImmutable;

    @NonTransitivelyImmutableField("There is only a non transitively immutable type parameter and no better one")
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,
            FinalClassWithNonTransitivelyImmutableField, T> multipleNonTransitivelyImmutable;

    @DependentlyImmutableField(value = "The generic type parameter is not concreted", parameter = {"T"})
    @NonAssignableField("The field is final")
    private final Generic<T> singleDependentlyImmutable;

    @DependentlyImmutableField(value = "At least one generic type parameter is not concreted", parameter = {"T"})
    @NonAssignableField("The field is final")
    private final MultipleGeneric<T,T,T> multipleDependentUniform;

    @DependentlyImmutableField(value = "The generic type parameter T is not concreted", parameter = {"T"})
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,T, FinalClassWithNoFields> multipleDependent;

    @TransitivelyImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,
            FinalClassWithNoFields, FinalClassWithNoFields> multipleTransitive;

    public GenericFields(T t, Object o, FinalClassWithNoFields fcwnf){

        this.singleMutable = new Generic<>(new ClassWithMutableFields());
        this.multipleMutable = new MultipleGeneric<>(t, new ClassWithMutableFields(), fcwnf);

        this.singleNonTransitivelyImmutable =
                new Generic<>(new FinalClassWithNonTransitivelyImmutableField(new Object()));

        this.multipleNonTransitivelyImmutable =
                new MultipleGeneric<>(fcwnf,new FinalClassWithNonTransitivelyImmutableField(new Object()), t);

        this.singleDependentlyImmutable = new Generic<>(t);
        this.multipleDependentUniform = new MultipleGeneric<T,T,T>(t,t,t);
        this.multipleDependent = new MultipleGeneric<>(fcwnf, t, fcwnf);

        this.singleTransitive = new Generic<>(fcwnf);
        this.multipleTransitive = new MultipleGeneric<>(fcwnf, fcwnf, fcwnf);
    }
}
