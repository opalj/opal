/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
/**
 * Encompasses different cases of final fields in combinations with different modifiers.
 */
//@Immutable
@MutableType("Class is not final")
@TransitivelyImmutableClass("Encompasses different case")
public class NonAssignability {

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    public final Integer publicInteger = new Integer(5);

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    protected final Integer protectedInteger = new Integer(5);

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    final Integer packagePrivateInteger = new Integer(5);

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final and initialized directly")
    private final int privateIntDirectlyAssigned = 1;

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final and initialized through instance initializer")
    private final int privateIntAssignedToInstanceInitializer;

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final and initialized through constructor")
    private final int privateIntAssignedThroughConstructor;

    final int n = 5;

    // Instance initializer!
    {
        privateIntAssignedToInstanceInitializer = 1;
    }

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    public static final int publicStaticFinalInt = 5;

    //@Immutable
    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
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
        privateIntAssignedThroughConstructor =1;
    }
}
@MutableType("Class is mutable")
@org.opalj.fpcf.properties.immutability.classes.MutableClass("Class has a mutable field")
class MutableClass{
    public int n = 8;
}
