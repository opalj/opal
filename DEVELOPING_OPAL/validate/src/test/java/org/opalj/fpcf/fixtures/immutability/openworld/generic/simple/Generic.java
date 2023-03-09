/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.DependentlyImmutableType;

/**
 * Generic class with a generic field.
 */
@DependentlyImmutableType(value = "class is dependently immutable and final", parameter = {"T"})
@DependentlyImmutableClass(value = "class has a dependently immutable field", parameter = {"T"})
public final class Generic<T> {

    @DependentlyImmutableField(value = "field t has a generic type parameter", parameter= {"T"} )
    @NonAssignableField("field t is final")
    private final T t;

    public Generic(T t){this.t = t;}
}
