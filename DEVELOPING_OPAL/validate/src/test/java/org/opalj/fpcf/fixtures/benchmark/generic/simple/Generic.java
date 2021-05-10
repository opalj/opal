/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;

/**
 * Generic class with a field having a generic type.
 */
//@Immutable
@DependentImmutableType("class is dependently immutable and final")
@DependentlyImmutableClass("class has a dependently immutable field")
public final class Generic<T> {

    //@Immutable
    @DependentImmutableField(value = "field has a generic type parameter", parameter= {"T"} )
    @NonAssignableFieldReference("field is final")
    private final T t;

    public Generic(T t){this.t = t;}
}