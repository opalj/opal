/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;
//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;

/**
 * Class with multiple final fields with generic type parameters.
 */
//@Immutable
@DependentImmutableType("class dependently immutable and final")
@DependentlyImmutableClass("class has only dependent immutable fields")
public final class MultipleGeneric<A,B,C> {

    //@Immutable
    @DependentImmutableField(value = "field has the generic type parameter A", parameter = {"A"})
    @NonAssignableFieldReference("field is final")
    private final A a;

    //@Immutable
    @DependentImmutableField(value = "field has the generic type parameter B", parameter = {"B"})
    @NonAssignableFieldReference("field is final")
    private final B b;

    //@Immutable
    @DependentImmutableField(value = "field has the generic type parameter C", parameter = {"C"})
    @NonAssignableFieldReference("field is final")
    private final C c;

    public MultipleGeneric(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
