/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@TransitivelyImmutableType("")
@TransitivelyImmutableClass("")
public final class CloneNonAssignable {

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private int i;

    public CloneNonAssignable clone(){
        CloneNonAssignable c = new CloneNonAssignable();
        c.i = i;
        return c;
    }
}
