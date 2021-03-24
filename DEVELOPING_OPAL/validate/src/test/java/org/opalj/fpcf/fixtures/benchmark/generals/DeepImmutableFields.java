/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;

//@Immutable
@DeepImmutableType("")
@DeepImmutableClass("")
public final class DeepImmutableFields {

    //@Immutable
    @DeepImmutableField("Immutable Reference and Immutable Field Type")
    @ImmutableFieldReference("declared final field")
    private final FinalEmptyClass fec = new FinalEmptyClass();

    public FinalEmptyClass getFec() {
        return fec;
    }

    //@Immutable
    @DeepImmutableField("Immutable Reference and Immutable Field Type")
    @ImmutableFieldReference("effective immutable field")
    private FinalEmptyClass name = new FinalEmptyClass();

    //@Immutable
    @DeepImmutableField("immutable reference and deep immutable field type")
    @ImmutableFieldReference(value = "declared final field reference")
    private final FinalEmptyClass fec1;

    public DeepImmutableFields(FinalEmptyClass fec) {
        this.fec1 = fec;
    }

}





