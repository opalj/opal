/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class encompasses two cases of assigning non assignable fields.
 */
//@Immutable
@MutableType("Class is not final.")
@TransitivelyImmutableClass("Class has only a transitively immutable field.")
public class DifferentAssignmentPossibilitiesOfNonAssignableField {

    //@Immutable
    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    private final Object o;

    public DifferentAssignmentPossibilitiesOfNonAssignableField(int n) {
        this.o = new Integer(n);
    }

    public DifferentAssignmentPossibilitiesOfNonAssignableField() {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}