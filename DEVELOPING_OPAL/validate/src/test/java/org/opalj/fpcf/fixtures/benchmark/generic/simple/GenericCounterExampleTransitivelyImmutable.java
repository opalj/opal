/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

/**
 * Generic class with a only a transitively immutable field.
 */
//@Immutable
@TransitiveImmutableClass("class has only a transitively immutable field")
class GenericCounterExampleTransitivelyImmutable<T> {

    //@Immutable
    @TransitiveImmutableField("field is non-assignable and has a primitive type")
    @NonAssignableFieldReference("field is final")
    private final int n = 5;
}
