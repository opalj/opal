/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;

/**
 * Collection of more complex test methods for purity analyses that can only be analyzed
 * interprocedurally.
 *
 * @author Dominik Helm
 */
class Complex {

    // Fields for methods to depend on

    static final int staticFinal = 0;

    private static int staticNonFinal = 0;

    // Base methods for others to depend on

    @CompileTimePure("Returns final static field")
    @Pure(value = "Returns final static field",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public static int pureBase() {
        return staticFinal;
    }

    @SideEffectFree("Returns non-final static field")
    @Impure(value = "Returns non-final static field", analyses = L0PurityAnalysis.class)
    public static int sefBase() {
        return staticNonFinal;
    }

    @Impure("Modifies static field")
    public void impureBase(int a) {
        staticNonFinal = a;
    }

    // Methods depending on side-effect free method which are pure internally

    @SideEffectFree("Calls a side-effect free method")
    @Impure(value = "Calls side-effect free method", analyses = L0PurityAnalysis.class)
    public static int sef_0_0(int a) {
        return a + Complex.sefBase();
    }

    @SideEffectFree("Transitively calls a side-effect free method")
    @Impure(value = "Transitively calls side-effect free method", analyses = L0PurityAnalysis.class)
    public static int sef_0_1(int a, int b) {
        return sef_0_0(1) + sef_0_2(a - 1, b);
    }

    @SideEffectFree("Transitively calls a side-effect free method")
    @Impure(value = "Transitively calls side-effect free method", analyses = L0PurityAnalysis.class)
    public static int sef_0_2(int a, int b) {
        if (a < 0) {
            return 0;
        } else {
            return sef_0_1(sef_0_3(b), a);
        }
    }

    @SideEffectFree("Transitively calls a side-effect free method")
    @Impure(value = "Transitively calls side-effect free method", analyses = L0PurityAnalysis.class)
    public static int sef_0_3(int a) {
        return a - sef_0_0(a);
    }

    // Methods depending on impure method which are pure internally

    @Impure("Calls impure method")
    public int impure_0_0(int a) {
        impureBase(a);
        return a;
    }

    @Impure("Transitively calls impure method")
    public int impure_0_1(int a, int b) {
        return impure_0_0(1) + impure_0_2(a - 1, b);
    }

    @Impure("Transitively calls impure method")
    public int impure_0_2(int a, int b) {
        if (a < 0) {
            return 0;
        } else {
            return impure_0_1(impure_0_3(b), a);
        }
    }

    @Impure("Transitively calls impure method")
    public int impure_0_3(int a) {
        return a - impure_0_0(a);
    }

    // Methods depending on pure method which are side-effect free

    @DomainSpecificSideEffectFree(
            "Value accessed from array is non-deterministic, potential IndexOutOfBoundsException")
    @Impure(value = "Uses array entry", analyses = L0PurityAnalysis.class)
    public static int sef_1_0(int[] a) {
        return a[Complex.pureBase()];
    }

    @SideEffectFree("Uses static non-final field")
    @Impure(value = "Uses static non-final field", analyses = L0PurityAnalysis.class)
    public static int sef_1_1(int a) {
        return staticNonFinal + Complex.pureBase();
    }

    // Methods depending on pure method which are impure

    @Impure("Modifies static field")
    public static void impure_1_0() {
        staticNonFinal = Complex.pureBase();
    }

    @Impure("Synchronizes on non-local object")
    public int impure_1_1() {
        synchronized (System.out) {
            return Complex.pureBase();
        }
    }

    // Methods depending on side-effect free method which are impure

    @Impure("Modifies static field")
    public static void impure_2_0() {
        staticNonFinal = Complex.sefBase();
    }

    @Impure("Synchronizes on non-local object")
    public int impure_2_1() {
        synchronized (System.out) {
            return Complex.sefBase();
        }
    }
}
