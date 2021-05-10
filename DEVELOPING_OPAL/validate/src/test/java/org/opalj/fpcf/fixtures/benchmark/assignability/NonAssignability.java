/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@TransitivelyImmutableClass("")
public class NonAssignability {

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    public final Object publicObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    protected final Object protectedObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    final Object packagePrivateObject = new Object();

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("Initialized directly")
    private final int a = 1;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("Initialized through instance initializer")
    private final int b;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("Initialized through constructor")
    private final int c;

    final int n = 5;



    // Instance initializer!
    {
        b = 1;
    }

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    public static final int publicStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    protected static final int protectedStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    private static final int privateStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    static final int packagePrivateStaticFinalInt = 5;

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    public static final Object publicStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    protected static final Object protectedStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    private static final Object privateStaticFinalObject = new MutableClass();

    //@Immutable
    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableFieldReference("non-assignable due to final modifier")
    static final Object packagePrivateStaticFinalObject = new MutableClass();

    public NonAssignability() {
        c=1;
    }
}

class MutableClass{
    public int n = 8;
}
