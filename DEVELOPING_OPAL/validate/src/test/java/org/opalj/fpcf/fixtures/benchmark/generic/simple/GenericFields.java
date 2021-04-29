/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.classes.NonTransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.fpcf.properties.immutability.types.TransitiveImmutableType;
import org.scalacheck.Gen;

/**
 * Class with multiple possibilities of generic types in combination with immutability.
 */
//@Immutable
@MutableType("")
@NonTransitiveImmutableClass("")
public class GenericFields<T> {

    //@Immutable
    @NonTransitivelyImmutableField("at least one generic type parameter is concretized with a mutable type")
    @NonAssignableFieldReference("field is final")
    private final Generic<ClassWithMutableFields> singleMutable;

    //@Immutable
    @NonTransitivelyImmutableField("at least one generic type parameter is conretized with a mutable type")
    @NonAssignableFieldReference("field is final")
    private final MultipleGeneric<T,ClassWithMutableFields,FinalClassWithNoFields> multipleMutable;

    //@Immutable
    @NonTransitivelyImmutableField("only a non transitively immutable type parameter")
    @NonAssignableFieldReference("field is final")
    private final Generic<FinalClassWithNonTransitivelyImmutableField> singleNonTransitive;

    //@Immutable
    @NonTransitivelyImmutableField("only a non transitively immutable type parameter and no better one")
    @NonAssignableFieldReference("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,
            FinalClassWithNonTransitivelyImmutableField, T> multipleNonTransitive;

    //@Immutable
    @DependentImmutableField("The generic type parameter is not concretized")
    @NonAssignableFieldReference("field is final")
    private final Generic<T> singleDependent;

    //@Immutable
    @DependentImmutableField("At least one generic type parameter is not concretized")
    @NonAssignableFieldReference("field is final")
    private final MultipleGeneric<T,T,T> multipleDependentUniform;

    //@Immutable
    @DependentImmutableField("At least one generic type parameter is not concretized")
    @NonAssignableFieldReference("field is final")
    private final MultipleGeneric<FinalClassWithNoFields,T, FinalClassWithNoFields> multipleDependent;

    //@Immutable
    @TransitiveImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableFieldReference("field is final")
    private final Generic<FinalClassWithNoFields> singleTransitive;

    //@Immutable
    @TransitiveImmutableField("generic type has only transitively immutable type parameters")
    @NonAssignableFieldReference("field is final")
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

@TransitiveImmutableType("Class has no fields and is final")
final class FinalClassWithNoFields {}

@NonTransitiveImmutableType("class has only one non transitively immutable field and is final")
final class FinalClassWithNonTransitivelyImmutableField{
    private final Object o;
    public FinalClassWithNonTransitivelyImmutableField(Object o){
        this.o = o;
    }
}

