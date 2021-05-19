/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;

//@Immutable
@NonTransitiveImmutableType("")
@NonTransitivelyImmutableClass("")
public final class EffectivelyNonAssignable {

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private int n = 5;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private Object escapingViaConstructor;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("field is only assigned once")
    private static int staticInt = 5;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("field is only assigned once")
    private static Object staticObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private Object escapingViaGetter = new Object();

    public EffectivelyNonAssignable(Object o) {
        this.escapingViaConstructor = o;
    }

    public Object getObject(){
        return escapingViaGetter;
    }
}
