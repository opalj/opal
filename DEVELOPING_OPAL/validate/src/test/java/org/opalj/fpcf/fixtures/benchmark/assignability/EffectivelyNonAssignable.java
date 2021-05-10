/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;

//@Immutable
@NonTransitiveImmutableType("")
@NonTransitivelyImmutableClass("")
public final class EffectivelyNonAssignable {

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private int n = 5;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private Object escapingViaConstructor;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("field is only assigned once")
    private static int staticInt = 5;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("field is only assigned once")
    private static Object staticObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private Object escapingViaGetter = new Object();

    public EffectivelyNonAssignable(Object o) {
        this.escapingViaConstructor = o;
    }

    public Object getObject(){
        return escapingViaGetter;
    }
}
