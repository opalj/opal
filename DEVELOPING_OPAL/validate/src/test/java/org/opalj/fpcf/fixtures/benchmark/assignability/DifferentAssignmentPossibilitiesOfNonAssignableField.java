/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.generals.EmptyClass;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class DifferentAssignmentPossibilitiesOfNonAssignableField {

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private final Object o;

    public DifferentAssignmentPossibilitiesOfNonAssignableField() {
        this.o = new EmptyClass();
    }

    public DifferentAssignmentPossibilitiesOfNonAssignableField(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}