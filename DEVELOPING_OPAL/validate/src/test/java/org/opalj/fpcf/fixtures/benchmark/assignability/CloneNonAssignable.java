/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;

//@Immutable
@DeepImmutableType("")
@DeepImmutableClass("")
public final class CloneNonAssignable {

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    int i;

    public CloneNonAssignable clone(){
        CloneNonAssignable c = new CloneNonAssignable();
        c.i = i;
        return c;
    }
}
