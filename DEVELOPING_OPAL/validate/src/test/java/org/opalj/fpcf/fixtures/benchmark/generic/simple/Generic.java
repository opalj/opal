/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;

/**
 * Generic class with a field having a generic type.
 */
//@Immutable
@DependentImmutableType(value = "class is dependently immutable and final", parameter = {"T"})
@DependentlyImmutableClass(value = "class has a dependently immutable field", parameter = {"T"})
public final class Generic<T> {

    //@Immutable
    @DependentImmutableField(value = "field has a generic type parameter", parameter= {"T"} )
    @NonAssignableField("field is final")
    private final T t;

    public Generic(T t){this.t = t;}
}
