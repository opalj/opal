/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
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
    @AssignableField("field is public and as a result assignable")
    public int n = 5;
}
