/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pureness;

/**
 * Some Demo code to test/demonstrate the complexity related to calculating the purity of
 * methods in the presence of mutual recursive methods.
 *
 * @author Michael Eichberg
 *
 */
class Demo { // This class is immutable; hence, instance methods _can be_ pure!

    private static int myValue = -1; /* the FieldMutabilityAnalysis is required to determine that this field is effectivelyFinal  */

    private Demo() {
        /* empty */
    }

    public Demo pureIdentity() {
        return this;
    }

    public static int pureUsesEffectivelyFinalField(int i, int j) {
        return i % j * myValue;
    }

    public static int pureSimpleRecursiveCall(int i, int j) {
        return i % 3 == 0 ? pureSimpleRecursiveCall(i, 0) : pureSimpleRecursiveCall(0, j);
    }

    public static int impureCallsSystemFunction(int i) {
        return (int) (i * System.nanoTime());
    }

    // --------------------------------------------------------------------------------------------
    // The following two methods are mutually dependent and are pure.
    //
    static int pureMutualRecursiveCall1(int i) {
        return i < 0 ? i : pureMutualRecursiveCall2(i - 10);
    }

    static int pureMutualRecursiveCall2(int i) {
        return i % 2 == 0 ? i : pureMutualRecursiveCall1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // The following methods are not directly involved in a  mutually recursive dependency, but
    // require information about a set of mutually recursive dependent methods.
    static int pureCallsMutuallyRecursivePureMethods(int i) { // also observed by other methods
        return pureMutualRecursiveCall1(i) + pureMutualRecursiveCall2(i);
    }

    static int pureUnusedCallsMutuallyRecursivePureMethods(int i) {
        return pureMutualRecursiveCall1(i) + pureMutualRecursiveCall2(i);
    }

    // --------------------------------------------------------------------------------------------
    // The following two methods are mutually dependent and use an impure method.
    //

    static int impureMutuallyRecursiveCallCallsImpure1(int i) {
        return i < 0 ?
                pureSimpleRecursiveCall(i, 0) :
                    impureMutuallyRecursiveCallCallsImpure2(i - 10);
    }

    static int impureMutuallyRecursiveCallCallsImpure2(int i) {
        return i % 2 == 0 ?
                impureCallsSystemFunction(i) :
                    impureMutuallyRecursiveCallCallsImpure1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // All three methods are actually pure but have a dependency on each other...
    //
    static int pureCyclicRecursiveCall1(int i) {
        return i < 0 ? i : pureCyclicRecursiveCall2(i - 10);
    }

    static int pureCyclicRecursiveCall2(int i) {
        return i % 2 == 0 ? i : pureCyclicRecursiveCall3(i - 1);
    }

    static int pureCyclicRecursiveCall3(int i) {
        return i % 4 == 0 ? i : pureCyclicRecursiveCall1(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // The following methods are pure, but only if we know the pureness of the target method
    // which we don't know if do not analyze the JDK!
    //

    static int cpureCallsAbs(int i) {
        return Math.abs(i) * 21;
    }

    static int cpureCallsAbsCallee(int i) {
        return cpureCallsAbs(i / 21);
    }

    static int cpureCallsAbsCalleeCallee1(int i) {
        return cpureCallsAbsCallee(i / 21);
    }

    static int cpureCallsAbsCalleeCallee2(int i) {
        return cpureCallsAbsCallee(i / 21);
    }

    static int cpureCallsAbsCalleeCalleeCalle(int i) {
        return cpureCallsAbsCalleeCallee1(i / 21) * cpureCallsAbsCalleeCallee2(i / 21);
    }

    static int cpureCallsAbsCalleeCalleeCalleCallee(int i) {
        return cpureCallsAbsCalleeCalleeCalle(1299);
    }

    // --------------------------------------------------------------------------------------------
    // All methods are involved in multiple cycles of dependent methods; one calls an impure method.
    //

    static int impureRecursiveCallWithDependency1(int i) {
        return i < 0 ? i : impureRecursiveCallWithDependency2(i - 10);
    }

    static int impureRecursiveCallWithDependency2(int i) {
        return i % 2 == 0 ? impureRecursiveCallWithDependency1(-i) : impureRecursiveCallWithDependency3(i - 1);
    }

    static int impureRecursiveCallWithDependency3(int i) {
        int j = pureCyclicRecursiveCall3(i);
        int k = impureRecursiveCallWithDependency1(j);
        int l = impureCallsSystemFunction(k);
        return pureCyclicRecursiveCall1(k);
    }

    static int impureComplex1(int i) {
        int j = impureComplex2(i-1);
        return impureComplex3(j*2);
    }

    static int impureComplex2(int i) {
        int j = impureComplex2(i-1);
        return impureComplex1(j*2);
    }

    static int impureComplex3(int i) {
        int j = impureComplex4(i-1);
        return impureComplex1(j*2);
    }

    static int impureComplex4(int i) {
        int j = impureComplex5(i-1);
        return impureComplex2(j*2);
    }

    static int impureComplex5(int i) {
        int j = impureComplex6(i-1);
        return impureComplex4(j*2);
    }

    static int impureComplex6(int i) {
        int j = impureComplex6(i-1);
        return impureAtLast(impureComplex4(j*2));
    }

    // --------------------------------------------------------------------------------------------
    // Two methods which are mutually dependent, but one depends on another pure method (where
    // the latter is also part of a mutual recursive dependency.
    //

    static int pureRecursiveCallWithDependency1(int i) {
        return i < 0 ? i : pureRecursiveCallWithDependency2(i - 10);
    }

    static int pureRecursiveCallWithDependency2(int i) {
        return i % 2 == 0 ?
                pureRecursiveCallWithDependency1(-i) :
                    pureCallsMutuallyRecursivePureMethods(i - 1);
    }

    // --------------------------------------------------------------------------------------------
    // All methods call directly or indirectly each other; but multiple cycles exist.
    //
    static int pureClosedSCC0(int i) {
        return i < 0 ? pureClosedSCC2(i - 10) : pureClosedSCC1(i - 111);
    }

    static int pureClosedSCC1(int i) {
        return i % 2 == 0 ? 32424 : pureClosedSCC3(i - 1);
    }

    static int pureClosedSCC2(int i) {
        return i % 2 == 0 ? 1001 : pureClosedSCC3(i - 3);
    }

    static int pureClosedSCC3(int i) {
        return pureClosedSCC0(12121 / i);
    }

    // --------------------------------------------------------------------------------------------
    // Impure, but takes "comparatively long to analyze"
    //
    public static int impureAtLast(int i) {
        int v = cpureCallsAbsCalleeCalleeCalleCallee(i);
        int u = impureRecursiveCallWithDependency1(impureRecursiveCallWithDependency2(v));
        int z = pureRecursiveCallWithDependency2(pureRecursiveCallWithDependency1(u));
        int l = pureClosedSCC2(pureClosedSCC1(pureClosedSCC0(z)));
        int j = impureComplex3(impureComplex2(impureComplex1(l)));
        int k = impureComplex6(impureComplex5(impureComplex4(j)));
        return (int) (k * System.nanoTime());
    }
}
