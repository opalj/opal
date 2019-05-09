/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;

/**
 * Collection of simple test methods using primitive types for purity analyses that can be analyzed
 * intraprocedurally.
 *
 * @author Dominik Helm
 */
class PrimitiveTypes {

    // Fields with different attributes for use in test methods

    private final int finalField = (int) System.nanoTime();

    private int effectivelyFinalField = 2;

    private int nonFinalField = 7;

    private final static int finalStaticField = (int) System.nanoTime();

    public static int nonFinalStaticField = 3;

    // Reading (effectively) final fields is pure, reading non-final fields is side-effect free,
    // writing fields is impure

    @CompileTimePure("Uses final field")
    @Pure(value = "Uses final field", analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
    public int getScaledFinalField() {
        return 2 * finalField;
    }

    @CompileTimePure(value = "Uses effectively final field",
            eps = @EP(cf = PrimitiveTypes.class, field = "effectivelyFinalField",
                    pk = "FieldMutability", p = "EffectivelyFinalField"))
    @Pure(value = "Uses effectively final field",
            eps = @EP(cf = PrimitiveTypes.class, field = "effectivelyFinalField",
                    pk = "FieldMutability", p = "EffectivelyFinalField"),
            analyses = L1PurityAnalysis.class)
    @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
    public int getScaledEffectivelyFinalField() {
        return effectivelyFinalField / 2;
    }

    @SideEffectFree("Uses non-final field")
    @Impure(value = "Uses non-final field", analyses = L0PurityAnalysis.class)
    public int getNegatedNonFinalField() {
        return -nonFinalField;
    }

    @SideEffectFree("Uses non-final static field")
    @Impure(value = "Uses non-final static field", analyses = L0PurityAnalysis.class)
    public static int getNonFinalStaticField() {
        return nonFinalStaticField;
    }

    @ContextuallyPure(value = "Modifies instance field", modifies = {0})
    @Impure(value = "Modifies instance field",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public void setNonFinalField(int newValue) {
        nonFinalField = newValue;
    }

    @ContextuallyPure(value = "Only modifies field of parameter", modifies = {1})
    @Impure(value="Modifies field of different instance",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public static void setNonFinalFieldStatic(PrimitiveTypes other, int newValue) {
        if(other != null) {
            other.nonFinalField = newValue;
        }
    }

    @Impure("Modifies static field")
    public static void setNonFinalStaticField(int newValue) {
        nonFinalStaticField = newValue;
    }

    // Methods which are pure internally are side-effect free if they depend on non-final fields

    @SideEffectFree("Uses non-final field")
    @Impure(value = "Uses non-final field", analyses = L0PurityAnalysis.class)
    public int sef_0_0() {
        return 2 * nonFinalField;
    }

    @SideEffectFree("Uses non-final static field")
    @Impure(value = "Uses non-final static field", analyses = L0PurityAnalysis.class)
    public static int sef_0_1(int a) {
        return a + nonFinalStaticField;
    }

    // Side-effect free Methods depending on final fields

    @DomainSpecificSideEffectFree(
            "Value accessed from array is non-deterministic, potential index out of bounds exception" )
    @Impure(value = "Accesses value from array", analyses = L0PurityAnalysis.class)
    public int sef_1_0(int[] arr) {
        return arr[finalField];
    }

    @DomainSpecificSideEffectFree(
            "Value accessed from array is non-deterministic, potential index out of bounds exception")
    @Impure(value = "Accesses value from array", analyses = L0PurityAnalysis.class)
    public static int sef_1_1(int[] arr) {
        return arr[finalStaticField];
    }

    // Impure methods depending on final fields

    @ContextuallyPure(value = "Modifies instance field", modifies = {0})
    @Impure(value = "Modifies instance field",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public void impure_0_0() {
        nonFinalField = finalField / 2;
    }

    @Impure("Modifies static field")
    public static void impure_0_1(int a) {
        nonFinalStaticField = a % finalStaticField;
    }

    // Some methods have a known purity level even if they can not be analyzed
    // (defined in ai/reference.conf)

    @SideEffectFree("Invokes known to be side-effect free native method")
    @Impure(value = "Analysis does not support preloaded purity values",
            analyses = L0PurityAnalysis.class)
    public long getCurrentTime(){
        return System.nanoTime();
    }
}
