/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Generic class with only a mutable field.
 */
//@Immutable
@MutableType("class is mutable")
@MutableClass("class has a mutable field")
class GenericCounterExampleMutable<T> {

    //@Immutable
    @MutableField("field is assignable")
    @AssignableFieldReference("field is assignable because it is public")
    public int n = 5;
}
