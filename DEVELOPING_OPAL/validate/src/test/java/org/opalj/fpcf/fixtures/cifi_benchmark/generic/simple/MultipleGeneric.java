/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.generic.simple;
//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;

/**
 * Class with multiple final fields with generic types.
 */
//@Immutable
@DependentImmutableType(value = "class is dependently immutable and final", parameter = {"A", "B", "C"})
@DependentlyImmutableClass(value = "class has only dependent immutable fields", parameter = {"A", "B", "C"})
public final class MultipleGeneric<A,B,C> {

    //@Immutable
    @DependentlyImmutableField(value = "field has the generic type parameter A", parameter = {"A"})
    @NonAssignableField("field is final")
    private final A a;

    //@Immutable
    @DependentlyImmutableField(value = "field has the generic type parameter B", parameter = {"B"})
    @NonAssignableField("field is final")
    private final B b;

    //@Immutable
    @DependentlyImmutableField(value = "field has the generic type parameter C", parameter = {"C"})
    @NonAssignableField("field is final")
    private final C c;

    public MultipleGeneric(A a, B b, C c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
