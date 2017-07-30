/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import annotations.purity.Purity;

import static annotations.purity.PurityKeys.*;

/**
 * Collection of more complex test methods for purity analyses that can only be analyzed
 * interprocedurally.
 *
 * @author Dominik Helm
 */
class Complex {

    // Fields for methods to depend on

    static final int staticFinal = 0;

    static int staticEffectivelyFinal = 0;

    static int staticNonFinal = 0;

    // Base methods for others to depend on

    @Purity(Pure) public static int pureBase() {
        return staticFinal;
    }

    @Purity(SideEffectFree) public static int sefBase() {
        return staticNonFinal;
    }

    @Purity(Impure) public void impureBase(int a) {
        staticNonFinal = a;
    }

    // Methods depending on side-effect free method which are pure internally

    @Purity(SideEffectFree) public static int sef_0_0(int a) {
        return a + Complex.sefBase();
    }

    @Purity(SideEffectFree) public static int sef_0_1(int a, int b) {
        return sef_0_0(1) + sef_0_2(a - 1, b);
    }

    @Purity(SideEffectFree) public static int sef_0_2(int a, int b) {
        if (a < 0) {
            return 0;
        } else {
            return sef_0_1(sef_0_3(b), a);
        }
    }

    @Purity(SideEffectFree) public static int sef_0_3(int a) {
        return a - sef_0_0(a);
    }

    // Methods depending on impure method which are pure internally

    @Purity(Impure) public int impure_0_0(int a) {
        impureBase(a);
        return a;
    }

    @Purity(Impure) public int impure_0_1(int a, int b) {
        return impure_0_0(1) + impure_0_2(a - 1, b);
    }

    @Purity(Impure) public int impure_0_2(int a, int b) {
        if (a < 0) {
            return 0;
        } else {
            return impure_0_1(impure_0_3(b), a);
        }
    }

    @Purity(Impure) public int impure_0_3(int a) {
        return a - impure_0_0(a);
    }

    // Methods depending on pure method which are side-effect free

    @Purity(SideEffectFree) public static int sef_1_0(int[] a) {
        return a[Complex.pureBase()];
    }

    @Purity(SideEffectFree) public static int sef_1_1(int a) {
        return staticEffectivelyFinal + Complex.pureBase();
    }

    // Methods depending on pure method which are impure

    @Purity(Impure) public static void impure_1_0() {
        staticNonFinal = Complex.pureBase();
    }

    @Purity(Impure) public int impure_1_1() {
        synchronized (this) {
            return Complex.pureBase();
        }
    }

    // Methods depending on side-effect free method which are impure

    @Purity(Impure) public static void impure_2_0() {
        staticNonFinal = Complex.sefBase();
    }

    @Purity(Impure) public int impure_2_1() {
        synchronized (this) {
            return Complex.sefBase();
        }
    }
}
