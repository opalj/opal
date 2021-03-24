/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;

@MutableClass("")
class GenericCounterExampleMutable<T> {

    //@Immutable
    public int n = 5;
}
