/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@TransitivelyImmutableClass("")
public class DifferentAssignmentPossibilitiesOfNonAssignableField {

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private final Object o;

    public DifferentAssignmentPossibilitiesOfNonAssignableField() {
        this.o = new C();
    }

    public DifferentAssignmentPossibilitiesOfNonAssignableField(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}

@TransitivelyImmutableClass("empty class")
class C {}