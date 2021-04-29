/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;
//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class with multiple final fields with generic type parameters.
 */
//@Immutable
@DependentImmutableType("class dependently immutable and final")
@DependentImmutableClass("class has only dependent immutable fields")
public final class MultipleGeneric<A,B,C> {

    //@Immutable
    @DependentImmutableField("field has the generic type parameter A")
    @NonAssignableFieldReference("field is final")
    private final A a;

    //@Immutable
    @DependentImmutableField("field has the generic type parameter B")
    @NonAssignableFieldReference("field is final")
    private final B b;

    //@Immutable
    @DependentImmutableField("field has the generic type parameter C")
    @NonAssignableFieldReference("field is final")
    private final C c;

    public MultipleGeneric(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
