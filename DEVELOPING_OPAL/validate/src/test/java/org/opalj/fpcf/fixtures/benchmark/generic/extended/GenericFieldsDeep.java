/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
class GenericFieldsDeep<T> {

    @DeepImmutableField("")
    private final Generic<Generic<FinalEmptyClass>> nestedDeep = new Generic<>(new Generic<>(new FinalEmptyClass()));

    @DeepImmutableField("")
    private final Generic<FinalEmptyClass> fecG = new Generic<>(new FinalEmptyClass());

}

final class FinalEmptyClass {}



