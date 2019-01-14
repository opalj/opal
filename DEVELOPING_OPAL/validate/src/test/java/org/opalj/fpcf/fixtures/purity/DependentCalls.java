/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.CompileTimePure;
import org.opalj.fpcf.properties.purity.EP;
import org.opalj.fpcf.properties.purity.Impure;
import org.opalj.fpcf.properties.purity.Pure;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;

/**
 * Some Demo code to test/demonstrate the complexity related to calculating the purity of
 * methods in the presence of mutual recursive methods.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
final class DependentCalls { // This class is immutable

    private static int myValue =
            -1; /* the FieldMutabilityAnalysis is required to determine that this field is effectivelyFinal  */

    @CompileTimePure("nothing done here")
    @Pure(value = "nothing done here",
            eps = @EP(cf = Object.class, method = "<init>()V", pk = "Purity",
                    p = "Pure", analyses = L0PurityAnalysis.class ),
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    @Impure(value = "Object.init<> not recognized as pure",
            eps = @EP(cf = Object.class, method = "<init>()V", pk = "Purity",
                    p = "Pure"),
            negate = true, analyses = L0PurityAnalysis.class)
    private DependentCalls() {
        /* empty */
    }

    @CompileTimePure("only constructs an object of this immutable class")
    @Pure(value = "only constructs an object of this immutable class",
            analyses = L1PurityAnalysis.class)
    @Impure(value = "Instantiates new object", analyses = L0PurityAnalysis.class)
    public static DependentCalls createDependentCalls() {
        return new DependentCalls();
    }

    @CompileTimePure(value = "object returned is immutable",
            eps = @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "ImmutableObject"))
    @Pure(value = "object returned is immutable",
            eps = @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "ImmutableObject"),
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    @Impure(value = "object returend not recognized as immutable",
            eps = @EP(cf = DependentCalls.class, pk = "ClassImmutability", p = "ImmutableObject"),
            negate = true, analyses = L1PurityAnalysis.class)
    public DependentCalls pureIdentity() {
        return this;
    }

    @Pure(value = "field used is effectively final",
            eps = @EP(cf = DependentCalls.class, field = "myValue", pk = "FieldMutability",
                    p = "EffectivelyFinalField"))
    @Impure(value = "field used not recognized as effectively final",
            eps = @EP(cf = DependentCalls.class, field = "myValue", pk = "FieldMutability",
                    p = "EffectivelyFinalField"),
            negate = true, analyses = L0PurityAnalysis.class)
    public static int pureUsesEffectivelyFinalField(int i, int j) {
        return i * j * myValue;
    }

    @CompileTimePure("only calls itself recursively")
    @Pure(value = "only calls itself recursively",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public static int pureSimpleRecursiveCall(int i, int j) {
        return i == 0 ? pureSimpleRecursiveCall(i, 0) : pureSimpleRecursiveCall(0, j);
    }

    @Impure("calls native function System.nanoTime()")
    public static int impureCallsSystemFunction(int i) {
        return (int) (i * System.getenv().size());
    }

    // --------------------------------------------------------------------------------------------
    // The following two methods are mutually dependent and are pure.
    //
    @CompileTimePure("function called is pure")
    @Pure(value = "function called is pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureMutualRecursiveCall1(int i) {
        return i < 0 ? i : pureMutualRecursiveCall2(i - 10);
    }

    @CompileTimePure("function called is pure")
    @Pure(value = "function called is pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureMutualRecursiveCall2(int i) {
        return i == 0 ? i : pureMutualRecursiveCall1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // The following methods are not directly involved in a  mutually recursive dependency, but
    // require information about a set of mutually recursive dependent methods.

    @CompileTimePure("functions called are compile-time pure")
    @Pure(value = "functions called are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureCallsMutuallyRecursivePureMethods(int i) { // also observed by other methods
        return pureMutualRecursiveCall1(i) + pureMutualRecursiveCall2(i);
    }

    @CompileTimePure("functions called are compile-time pure")
    @Pure(value = "functions called are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureUnusedCallsMutuallyRecursivePureMethods(int i) {
        return pureMutualRecursiveCall1(i) + pureMutualRecursiveCall2(i);
    }

    // --------------------------------------------------------------------------------------------
    // The following two methods are mutually dependent and use an impure method.
    //

    @Impure("transitively calls native function")
    static int impureMutuallyRecursiveCallCallsImpure1(int i) {
        return i < 0 ?
                pureSimpleRecursiveCall(i, 0) :
                impureMutuallyRecursiveCallCallsImpure2(i - 10);
    }

    @Impure("transitively calls native function")
    static int impureMutuallyRecursiveCallCallsImpure2(int i) {
        return i % 2 == 0 ?
                impureCallsSystemFunction(i) :
                impureMutuallyRecursiveCallCallsImpure1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // All three methods are actually pure but have a dependency on each other...
    //
    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureCyclicRecursiveCall1(int i) {
        return i < 0 ? i : pureCyclicRecursiveCall2(i - 10);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureCyclicRecursiveCall2(int i) {
        return i == 0 ? i : pureCyclicRecursiveCall3(i - 1);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureCyclicRecursiveCall3(int i) {
        return i > 0 ? i : pureCyclicRecursiveCall1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // The following methods are pure, but only if we know the pureness of the target method
    // which we don't know if do not analyze the JDK!
    //

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbs(int i) {
        return Math.abs(i) * 21;
    }

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbsCallee(int i) {
        return cpureCallsAbs(i + 21);
    }

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbsCalleeCallee1(int i) {
        return cpureCallsAbsCallee(i - 21);
    }

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbsCalleeCallee2(int i) {
        return cpureCallsAbsCallee(i * 21);
    }

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbsCalleeCalleeCallee(int i) {
        return cpureCallsAbsCalleeCallee1(i + 21) * cpureCallsAbsCalleeCallee2(i - 21);
    }

    @CompileTimePure(value = "calls compile-time pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "CompileTimePure"))
    @Pure(value = "calls pure Math.abs",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Pure"))
    @Impure(value = "Math.abs not recognized as pure",
            eps = @EP(cf = Math.class, method = "abs(I)I", pk = "Purity", p = "Impure"))
    static int cpureCallsAbsCalleeCalleeCalleCallee(int i) {
        return cpureCallsAbsCalleeCalleeCallee(1299);
    }

    // --------------------------------------------------------------------------------------------
    // All methods are involved in multiple cycles of dependent methods; one calls an impure method.
    //

    @Impure("transitively calls native function")
    static int impureRecursiveCallWithDependency1(int i) {
        return i < 0 ? i : impureRecursiveCallWithDependency2(i - 10);
    }

    @Impure("transitively calls native function")
    static int impureRecursiveCallWithDependency2(int i) {
        return i % 2 == 0 ?
                impureRecursiveCallWithDependency1(-i) :
                impureRecursiveCallWithDependency3(i - 1);
    }

    @Impure("transitively calls native function")
    static int impureRecursiveCallWithDependency3(int i) {
        int j = pureCyclicRecursiveCall3(i);
        int k = impureRecursiveCallWithDependency1(j);
        int l = impureCallsSystemFunction(k);
        return pureCyclicRecursiveCall1(l);
    }

    @Impure("transitively calls native function")
    static int impureComplex1(int i) {
        int j = impureComplex2(i - 1);
        return impureComplex3(j * 2);
    }

    @Impure("transitively calls native function")
    static int impureComplex2(int i) {
        int j = impureComplex2(i - 1);
        return impureComplex1(j * 2);
    }

    @Impure("transitively calls native function")
    static int impureComplex3(int i) {
        int j = impureComplex4(i - 1);
        return impureComplex1(j * 2);
    }

    @Impure("transitively calls native function")
    static int impureComplex4(int i) {
        int j = impureComplex5(i - 1);
        return impureComplex2(j * 2);
    }

    @Impure("transitively calls native function")
    static int impureComplex5(int i) {
        int j = impureComplex6(i - 1);
        return impureComplex4(j * 2);
    }

    @Impure("transitively calls native function")
    static int impureComplex6(int i) {
        int j = impureComplex6(i - 1);
        return impureAtLast(impureComplex4(j * 2));
    }

    // --------------------------------------------------------------------------------------------
    // Two methods which are mutually dependent, but one depends on another pure method (where
    // the latter is also part of a mutual recursive dependency.
    //

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCallWithDependency1(int i) {
        return i < 0 ? i : pureRecursiveCallWithDependency2(i - 10);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCallWithDependency2(int i) {
        return i == 0 ?
                pureRecursiveCallWithDependency1(-i) :
                pureCallsMutuallyRecursivePureMethods(i - 1);
    }

    //---------------------------------------------------------------------------------------------
    // More tests for several levels of dependency
    //

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCall2DependencyLevels1(int i) {
        return i > 0 ?
                pureRecursiveCallWithDependency1(i * 5) :
                pureRecursiveCall2DependencyLevels2(10 - i);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCall2DependencyLevels2(int i) {
        return i <= 0 ? 0 : pureRecursiveCall2DependencyLevels1(i * i);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCall3DependencyLevels1(int i) {
        return i == 1 ? i : pureRecursiveCall3DependencyLevels2(-i);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureRecursiveCall3DependencyLevels2(int i) {
        return i >= 0 ?
                pureRecursiveCall3DependencyLevels1(i + 100) :
                pureRecursiveCall2DependencyLevels2(100 - i);
    }

    // --------------------------------------------------------------------------------------------
    // All methods call directly or indirectly each other; but multiple cycles exist.
    //
    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureClosedSCC0(int i) {
        return i < 0 ? pureClosedSCC2(i - 10) : pureClosedSCC1(i - 111);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureClosedSCC1(int i) {
        return i == 0 ? 32424 : pureClosedSCC3(i - 1);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureClosedSCC2(int i) {
        return i > 0 ? 1001 : pureClosedSCC3(i - 3);
    }

    @CompileTimePure("methods in all cycles are compile-time pure")
    @Pure(value = "methods in all cycles are pure",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    static int pureClosedSCC3(int i) {
        return pureClosedSCC0(i * 12121);
    }

    // --------------------------------------------------------------------------------------------
    // Impure, but takes "comparatively long to analyze"
    //
    @Impure("calls native function System.nanoTime()")
    public static int impureAtLast(int i) {
        int v = cpureCallsAbsCalleeCalleeCalleCallee(i);
        int u = impureRecursiveCallWithDependency1(impureRecursiveCallWithDependency2(v));
        int z = pureRecursiveCallWithDependency2(pureRecursiveCallWithDependency1(u));
        int l = pureClosedSCC2(pureClosedSCC1(pureClosedSCC0(z)));
        int j = impureComplex3(impureComplex2(impureComplex1(l)));
        int k = impureComplex6(impureComplex5(impureComplex4(j)));
        return (int) (k * System.getenv().size());
    }
}
