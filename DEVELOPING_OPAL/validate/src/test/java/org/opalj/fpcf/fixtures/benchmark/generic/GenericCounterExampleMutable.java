/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;

@MutableClass("")
class GenericCounterExampleMutable<T> {

    //@Immutable
    @MutableField("")
    public int n = 5;
}
