/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * Class with multiple possibilities of generic types in combination with immutability.
 */
//@Immutable
@MutableType("")
@NonTransitivelyImmutableClass("")
public class GenericFields<T> {

    //@Immutable
    @NonTransitivelyImmutableField("at least one generic type parameter is concretized with a mutable type")
    @NonAssignableField("field is final")
    private final Generic<ClassWithMutableFields> singleMutable;

    //@Immutable
    @NonTransitivelyImmutableField("at least one generic type parameter is conretized with a mutable type")
    @NonAssignableField("field is final")
    private final MultipleGeneric<T,ClassWithMutableFields,FinalClassWithNoFields> multipleMutable;

    //@Immutable
    @NonTransitivelyImmutableField("only a non transitively immutable type parameter")
    @NonAssignableField("field is final")
    private final Generic<FinalClassWithNonTransitivelyImmutableField> singleNonTransitive;

    //@Immutable
    @NonTransitivelyImmutableField("only a non transitively immutable type parameter and no better one")
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,
            FinalClassWithNonTransitivelyImmutableField, T> multipleNonTransitive;

    //@Immutable
    @DependentImmutableField(value = "The generic type parameter is not concretized", parameter = {"T"})
    @NonAssignableField("field is final")
    private final Generic<T> singleDependent;

    //@Immutable
    @DependentImmutableField(value = "At least one generic type parameter is not concretized", parameter = {"T"})
    @NonAssignableField("field is final")
    private final MultipleGeneric<T,T,T> multipleDependentUniform;

    //@Immutable
    @DependentImmutableField(value = "At least one generic type parameter is not concretized", parameter = {"T"})
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,T, FinalClassWithNoFields> multipleDependent;

    //@Immutable
    @TransitivelyImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final Generic<FinalClassWithNoFields> singleTransitive;

    //@Immutable
    @TransitivelyImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableField("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,
            FinalClassWithNoFields, FinalClassWithNoFields> multipleTransitive;


    public GenericFields(T t, Object o, FinalClassWithNoFields fcwnf){

        this.singleMutable = new Generic(new ClassWithMutableFields());
        this.multipleMutable = new MultipleGeneric<>(t, new ClassWithMutableFields(), fcwnf);

        this.singleNonTransitive = new Generic(new FinalClassWithNonTransitivelyImmutableField(new Object()));

        this.multipleNonTransitive =
                new MultipleGeneric<>(fcwnf,new FinalClassWithNonTransitivelyImmutableField(new Object()), t);

        this.singleDependent = new Generic<>(t);
        this.multipleDependentUniform = new MultipleGeneric<T,T,T>(t,t,t);
        this.multipleDependent = new MultipleGeneric<>(fcwnf, t, fcwnf);

        this.singleTransitive = new Generic<>(fcwnf);
        this.multipleTransitive = new MultipleGeneric<>(fcwnf, fcwnf, fcwnf);
    }
}

@TransitivelyImmutableType("Class has no fields and is final")
final class FinalClassWithNoFields {}

@NonTransitiveImmutableType("class has only one non transitively immutable field and is final")
final class FinalClassWithNonTransitivelyImmutableField{
    private final Object o;
    public FinalClassWithNonTransitivelyImmutableField(Object o){
        this.o = o;
    }
}

