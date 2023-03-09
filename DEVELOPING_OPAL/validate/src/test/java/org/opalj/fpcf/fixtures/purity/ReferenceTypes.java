/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;

/**
 * Collection of simple test methods using object and array types for purity analyses that can be
 * analyzed intraprocedurally.
 *
 * @author Dominik Helm
 */
final public class ReferenceTypes {

    // Fields with different attributes for use in test methods

    private static final DependentCalls staticNonAssignableTransitivelyImmutableObj =
            DependentCalls.createDependentCalls();

    private static DependentCalls staticEffectivelyNonAssignableTransitivelyImmutableObj =
            DependentCalls.createDependentCalls();

    private static final Object staticNonAssignableObj = new Object();

    private static Object staticEffectivelyNonAssignableObj = new Object();

    private static Object staticAssignableObj = new Object();

    private final int[] nonConstNonAssignableArr = new int[]{1, 2, 3};

    private int[] constEffectivelyNonAssignableArr = new int[]{2, 3, 4};

    public int[] nonAssignableArr = new int[]{5, 6, 7};

    private int assignableField;

    // Constructors are pure even if they write fields of the currently created object
    // (Unless the object escapes the thread)
    // They are impure if they write other object's fields, though

    @CompileTimePure("Only initializes fields")
    @Pure(value = "Only initializes fields", analyses = L1PurityAnalysis.class)
    @Impure(value = "Sets array entries", analyses = L0PurityAnalysis.class)
    public ReferenceTypes() {
        assignableField = 5;
    }

    @ContextuallyPure(value = "Only modifies fields of parameters", modifies = {1})
    @Impure(value = "Modifies field of different instance",
            analyses = {L0PurityAnalysis.class, L1PurityAnalysis.class})
    public ReferenceTypes(ReferenceTypes other) {
        assignableField = 7;
        if (other != null) {
            other.assignableField = 27;
        }
    }

    // Using (effectively) non-assignable static fields is pure
    // Returning assignable references is side-effect free,
    // if the reference is not fresh and non-escaping
    // Using assignable static fields is side-effect free
    // Setting static fields is impure

    @Pure(value = "Uses non-assignable static immutable object",
            eps = @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "TransitivelyImmutableClass"),
            analyses = {L1PurityAnalysis.class, L2PurityAnalysis.class})
    @Impure(value = "DependentCalls not recognized as immutable",
            eps = @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "TransitivelyImmutableClass"),
            negate = true, analyses = L0PurityAnalysis.class)
    public static DependentCalls getStaticNonAssignableTransitivelyImmutableObj() {
        return staticNonAssignableTransitivelyImmutableObj;
    }

    @Pure(value = "Uses effectively non-assignable static immutable object", eps = {
            @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "TransitivelyImmutableClass"),
            @EP(cf = ReferenceTypes.class, field = "staticEffectivelyNonAssignableTransitivelyImmutableObj",
                    pk = "FieldImmutability", p = "NonTransitivelyImmutableField")
    })
    @Impure(value = "staticEffectivelyNonAssignableTransitivelyImmutableObj not recognized " +
            "as effectively final static immutable object",
            negate = true, eps = {
            @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "TransitivelyImmutableClass"),
            @EP(cf = ReferenceTypes.class, field = "staticEffectivelyNonAssignableTransitivelyImmutableObj",
                    pk = "FieldImmutability", p = "NonTransitivelyImmutableField")
    }, analyses = L0PurityAnalysis.class)
    public static DependentCalls getStaticEffectivelyNonAssignableTransitivelyImmutableObj() {
        return staticEffectivelyNonAssignableTransitivelyImmutableObj;
    }

    @SideEffectFree("Returns mutable object")
    @Impure(value = "Returns mutable object", analyses = L0PurityAnalysis.class)
    public static Object getStaticNonAssignableObj() {
        return staticNonAssignableObj;
    }

    @SideEffectFree("Returns mutable object")
    @Impure(value = "Uses mutable object", analyses = L0PurityAnalysis.class)
    public static Object getStaticEffectivelyNonAssignableObj() {
        return staticEffectivelyNonAssignableObj;
    }

    @SideEffectFree("Returns object from non-final field")
    @Impure(value = "Returns object from non-final field", analyses = L0PurityAnalysis.class)
    public static Object getStaticAssignableObj() {
        return staticAssignableObj;
    }

    @Impure("Modifies static field")
    public static void setStaticObj(Object newObject) {
        staticAssignableObj = newObject;
    }

    // Objects returned from methods are local if they are fresh and therefore the method can be
    // pure even if the object returned is mutable

    @CompileTimePure("Returns a new object")
    @Pure(value = "Returns a new object", analyses = L1PurityAnalysis.class)
    @Impure(value = "Allocates new object", analyses = L0PurityAnalysis.class)
    public static Object getNewObject() {
        return new Object();
    }


    @Pure(value = "Returns new object, but type (-> mutability) is not precisely known anymore",
            eps = @EP(cf = ReferenceTypes.class, pk = "ReturnValueFreshness",
            method = "getNewObject()Ljava/lang/Object;", p = "FreshReturnValue"))
    @SideEffectFree(value = "Anaylsis doesn't recognize new object/freshness not recognized",
            eps = @EP(cf = ReferenceTypes.class, pk = "ReturnValueFreshness",
                    method = "getNewObject()Ljava/lang/Object;", p = "FreshReturnValue",
                    analyses = L2PurityAnalysis.class), negate = true)
    @Impure(value = "Transitively allocates new object", analyses = L0PurityAnalysis.class)
    public static Object getNewObjectIndirect() {
        return getNewObject();
    }

    @SideEffectFree("Returns mutable object")
    @Impure(value = "Returns mutable object", analyses = L0PurityAnalysis.class)
    public static Object getStaticFinalObjIndirect() {
        return getStaticNonAssignableObj();
    }

    @Pure(value = "Returns a new object", analyses = L2PurityAnalysis.class,
            eps = @EP(cf = ReferenceTypes.class, pk = "ReturnValueFreshness",
                    method = "getNewObject()Ljava/lang/Object;", p = "FreshReturnValue"))
    @SideEffectFree(value = "Anaylsis doesn't recognize new object/freshness not recognized",
            eps = @EP(cf = ReferenceTypes.class, pk = "ReturnValueFreshness",
                    method = "getNewObject()Ljava/lang/Object;", p = "FreshReturnValue",
                    analyses = L2PurityAnalysis.class), negate = true)
    @Impure(value = "Allocates new object", analyses = L0PurityAnalysis.class)
    public static Object getNewObjectDirectOrIndirect(boolean b) {
        if (b) return new Object();
        else return getNewObject();
    }

    // If the returned object is not guaranteed to be fresh, returning mutable objects is not pure

    @SideEffectFree("Returns a potentially non-fresh object")
    @Impure(value = "Allocates new object", analyses = L0PurityAnalysis.class)
    public static Object getNonFreshObject(boolean b) {
        if (b) return new Object();
        else return staticNonAssignableObj;
    }

    @SideEffectFree("Returns a potentially non-fresh object")
    @Impure(value = "Transitively allocates new object", analyses = L0PurityAnalysis.class)
    public static Object getNonFreshObjectIndirect(boolean b) {
        return getNonFreshObject(b);
    }

    // Reading and writing fields of fresh objects is pure

    @CompileTimePure("Uses mutable field of fresh object")
    @Pure(value = "Uses mutable field of fresh object", analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
    public static int getFreshObjectField() {
        ReferenceTypes obj = new ReferenceTypes();
        return obj.assignableField;
    }

    @Pure(value = "Modifies field of fresh object",
            analyses = {L1PurityAnalysis.class, L2PurityAnalysis.class})
    @Impure(value = "Modifies instance field", analyses = L0PurityAnalysis.class)
    public static Object setFreshObjectField(int value) {
        ReferenceTypes obj = new ReferenceTypes();
        obj.assignableField = value;
        return obj;
    }

    // Using the array-length is pure, as it is immutable
    // Reading array entries is side-effect free
    // (not pure even for parameters because of concurrent modification)
    // Writing array entries is impure
    // Reading and writing entries of fresh arrays is pure, though
    // Array access may throw IndexOutOfBounds exceptions, but this is pure

    @CompileTimePure("Uses array length of final array")
    @Pure(value = "Uses array length of non assignable array", analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses array length", analyses = L0PurityAnalysis.class)
    public int getFinalArrLength() {
        int[] arr = nonConstNonAssignableArr;
        if (arr == null)
            return 0; // Work around unknown null-ness
        return arr.length;
    }

    @CompileTimePure(value = "Uses array length of effectively non-assignable array",
            eps = @EP(cf = ReferenceTypes.class, field = "constEffectivelyNonAssignableArr",
                    pk = "FieldImmutability", p = "NonTransitivelyImmutableField"))
    @Pure(value = "Uses array length of effectively non-assignable array",
            eps = @EP(cf = ReferenceTypes.class, field = "constEffectivelyNonAssignableArr",
                    pk = "FieldImmutability", p = "NonTransitivelyImmutableField"),
            analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses array length", analyses = L0PurityAnalysis.class)
    public int getEffectivelyFinalArrLength() {
        int[] arr = constEffectivelyNonAssignableArr;
        if (arr == null)
            return 0; // Work around unknown null-ness
        return arr.length;
    }

    @SideEffectFree("Uses array length")
    @Impure(value = "Uses array length", analyses = L0PurityAnalysis.class)
    public int getNonFinalArrLength() {
        int[] arr = nonAssignableArr;
        if (arr == null)
            return 0;
        return arr.length;
    }

    @CompileTimePure("Uses array length from parameter array")
    @Pure(value = "Uses array length from parameter array", analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses array length", analyses = L0PurityAnalysis.class)
    public static int getArrLengthStatic(int[] arr) {
        if (arr == null)
            return 0;
        return arr.length;
    }

    @DomainSpecificSideEffectFree(
            "Array entry used is non-deterministic, potential IndexOutOfBoundsException")
    @Impure(value = "Uses array entries", analyses = L0PurityAnalysis.class)
    public int getArrayEntries(int index) {
        return nonConstNonAssignableArr[index] + nonAssignableArr[index];
    }

    @DomainSpecificPure(value = "Potential IndexOutOfBoundsException", analyses = {})
    @DomainSpecificSideEffectFree(
            value = "This analysis doesn't recognize const-ness, potential IndexOutOfBoundsException",
            analyses = {L1PurityAnalysis.class, L2PurityAnalysis.class}
    )
    @Impure(value = "Uses array entry", analyses = L0PurityAnalysis.class)
    public int getConstArrayEntry(int index) {
        return constEffectivelyNonAssignableArr[index];
    }

    @DomainSpecificSideEffectFree(
            "Uses entry from parameter array, potential IndexOutOfBoundsException")
    @Impure(value = "Uses array entry", analyses = L0PurityAnalysis.class)
    public static int getArrayEntryStatic(int index, int[] arr) {
        return arr[index];
    }

    @DomainSpecificContextuallyPure(value = "Modified array is local", modifies = {0},
            eps = @EP(cf = ReferenceTypes.class, pk = "FieldLocality", field = "nonConstNonAssignableArr",
                    p = "LocalField"))
    @Impure(value = "Modifies array entry/array not recognized as local", negate = true,
            eps = @EP(cf = ReferenceTypes.class, pk = "FieldLocality", field = "nonConstNonAssignableArr",
                    p = "LocalField", analyses = L2PurityAnalysis.class))
    public void setArrayEntry(int index, int value) {
        nonConstNonAssignableArr[index] = value;
    }

    @DomainSpecificContextuallyPure(
            value = "Modifies parameter, array could be null or index out bounds", modifies = {1})
    @Impure(value = "Modifies array entry",
            analyses = {L0PurityAnalysis.class, L1PurityAnalysis.class})
    public static void setArrayEntryStatic(int[] arr, int index, int value) {
        arr[index] = value;
    }

    @CompileTimePure("Uses entries from fresh array")
    @Pure(value = "Uses entries from fresh array", analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses array entries", analyses = L0PurityAnalysis.class)
    public static int getFreshArrayEntry(int index) {
        int[] arr = new int[]{1, 2, 3};
        if (index > 0) {
            return arr[index % 3];
        }
        return 0;
    }

    @CompileTimePure("Modifies entries on fresh array")
    @Pure(value = "Modifies entries on fresh array", analyses = L1PurityAnalysis.class)
    @Impure(value = "Modifies array entry", analyses = L0PurityAnalysis.class)
    public static int[] setFreshArrayEntry(int index, int value) {
        int[] arr = new int[]{1, 2, 3};
        if (index > 0) {
            arr[index % 3] = value;
        }
        return arr;
    }

    @SideEffectFree("hashCode is not deterministic on new objects")
    @Impure(value = "Analysis doesn't recognize side-effect free methods",
            analyses = L0PurityAnalysis.class)
    private int newHashCode(){
        return new Object().hashCode();
    }

    // Synchronization is impure (unless done on a fresh, non-escaping object)

    @DomainSpecificContextuallyPure(value = "Synchronizes on this reference", modifies = {0})
    @Impure(value = "Synchronizes on this reference",
            analyses = {L0PurityAnalysis.class, L1PurityAnalysis.class})
    private int syncMethod() {
        int result;
        synchronized (this) {
            result = 5;
        }
        return result;
    }

    @ContextuallyPure(value = "Synchronizes on this reference", modifies = {0})
    @Impure(value = "Synchronizes on this reference",
            analyses = {L0PurityAnalysis.class, L1PurityAnalysis.class})
    private synchronized int syncMethod2() {
        return 5;
    }
}
