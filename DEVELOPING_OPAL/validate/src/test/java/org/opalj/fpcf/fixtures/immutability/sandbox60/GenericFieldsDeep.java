/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.sandbox60;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@TransitivelyImmutableClass("")
class GenericFieldsDeep<T> {

    @TransitivelyImmutableField("")
    private Generic<Generic<FinalEmptyClass>> nestedDeep = new Generic<>(new Generic<>(new FinalEmptyClass()));

    @TransitivelyImmutableField("")
    Generic<FinalEmptyClass> fecG = new Generic<>(new FinalEmptyClass());

}

final class FinalEmptyClass {}



