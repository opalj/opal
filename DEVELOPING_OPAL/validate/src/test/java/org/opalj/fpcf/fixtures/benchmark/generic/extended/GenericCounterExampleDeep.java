/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;

//@Immutable
@DeepImmutableClass("")
class GenericCounterExampleDeep<T> {

    //@Immutable
    @DeepImmutableField("")
    private final int n = 5;
}
