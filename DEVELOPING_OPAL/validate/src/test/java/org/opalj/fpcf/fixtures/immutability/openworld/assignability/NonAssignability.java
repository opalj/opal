/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
/**
 * Encompasses different cases of final fields in combinations with different modifiers.
 */
@MutableType("Class is not final")
@TransitivelyImmutableClass("Encompasses different case")
public class NonAssignability {

    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    public final Integer publicInteger = new Integer(5);

    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    protected final Integer protectedInteger = new Integer(5);

    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    final Integer packagePrivateInteger = new Integer(5);

    @TransitivelyImmutableField("Field is final and has a primitive type.")
    @NonAssignableField("Field is final and initialized directly")
    private final int privateIntDirectlyAssigned = 1;

    @TransitivelyImmutableField("Field is final and has a transitively immutable type.")
    @NonAssignableField("Field is final and initialized through instance initializer")
    private final int privateIntAssignedToInstanceInitializer;

    @TransitivelyImmutableField("Field is final and has a primitive type.")
    @NonAssignableField("Field is final and initialized through constructor")
    private final int privateIntAssignedThroughConstructor;

    final int n = 5;

    // Instance initializer!
    {
        privateIntAssignedToInstanceInitializer = 1;
    }

    @TransitivelyImmutableField("Field is final and has a primitive type.")
    @NonAssignableField("Field is final")
    public static final int publicStaticFinalInt = 5;

    @TransitivelyImmutableField("Field is final and has a primitive type.")
    @NonAssignableField("Field is final")
    protected static final int protectedStaticFinalInt = 5;

    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    private static final int privateStaticFinalInt = 5;

    @TransitivelyImmutableField("non-assignable field with primitive type")
    @NonAssignableField("non-assignable due to final modifier")
    static final int packagePrivateStaticFinalInt = 5;

    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    public static final Object publicStaticFinalObject = new MutClass();

    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    protected static final Object protectedStaticFinalObject = new MutClass();

    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    private static final Object privateStaticFinalObject = new MutClass();

    @NonTransitivelyImmutableField("non-assignable field with mutable type")
    @NonAssignableField("non-assignable due to final modifier")
    static final Object packagePrivateStaticFinalObject = new MutClass();

    public NonAssignability() {
        privateIntAssignedThroughConstructor =1;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class MutClass {
    public int n = 8;
}
