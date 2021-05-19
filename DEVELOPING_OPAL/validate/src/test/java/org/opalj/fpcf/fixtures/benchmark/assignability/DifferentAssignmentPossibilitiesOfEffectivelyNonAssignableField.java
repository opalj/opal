/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@TransitivelyImmutableClass("")
public class DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField {

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private Object o;

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField() {
        this.o = new Integer(5);
    }

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}
