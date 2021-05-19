/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@TransitivelyImmutableClass("")
public class NonAssignability {

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("")
    public final Object publicObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("")
    protected final Object protectedObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("")
    final Object packagePrivateObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("Initialized directly")
    private final int a = 1;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("Initialized through instance initializer")
    private final int b;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("Initialized through constructor")
    private final int c;

    final int n = 5;



    // Instance initializer!
    {
        b = 1;
    }

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    public static final int publicStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    protected static final int protectedStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    private static final int privateStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    static final int packagePrivateStaticFinalInt = 5;

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    public static final Object publicStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    protected static final Object protectedStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    private static final Object privateStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    static final Object packagePrivateStaticFinalObject = new MutableClass();

    public NonAssignability() {
        c=1;
    }
}

class MutableClass{
    public int n = 8;
}
