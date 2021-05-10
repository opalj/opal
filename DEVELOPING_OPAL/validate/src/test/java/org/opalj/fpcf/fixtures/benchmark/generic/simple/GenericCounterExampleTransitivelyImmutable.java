/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

/**
 * Generic class with a only a transitively immutable field.
 */
//@Immutable
@TransitivelyImmutableClass("class has only a transitively immutable field")
class GenericCounterExampleTransitivelyImmutable<T> {

    //@Immutable
    @TransitivelyImmutableField("field is non-assignable and has a primitive type")
    @NonAssignableFieldReference("field is final")
    private final int n = 5;
}
