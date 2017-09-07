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
 * Collection of simple test methods using primitive types for purity analyses that can be analyzed
 * intraprocedurally.
 *
 * @author Dominik Helm
 *
 */
class PrimitiveTypes {

    // Fields with different attributes for use in test methods

    private final int finalField = (int) System.nanoTime();

    private int effectivelyFinalField = 2;

    private int nonFinalField = 7;

    private final static int finalStaticField = (int) System.nanoTime();

    private static int nonFinalStaticField = 3;

    // Reading (effectively) final fields is pure, reading non-final fields is side-effect free,
    // writing fields is impure

    @Purity(Pure)
    public int getScaledFinalField(){
        return 2 * finalField;
    }

    @Purity(Pure)
    public int getScaledEffectivelyFinalField(){
        return effectivelyFinalField / 2;
    }

    @Purity(SideEffectFree)
    public int getNegatedNonFinalField(){
        return -nonFinalField;
    }

    @Purity(SideEffectFree)
    public static int getNonFinalStaticField(){
        return nonFinalStaticField;
    }

    @Purity(Impure)
    public void setNonFinalField(int newValue){
        nonFinalField = newValue;
    }

    @Purity(Impure)
    public static void setNonFinalFieldStatic(PrimitiveTypes other, int newValue){
        other.nonFinalField = newValue;
    }

    @Purity(Impure)
    public static void setNonFinalStaticField(int newValue){
        nonFinalStaticField = newValue;
    }

    // Methods depending on non-final fields which are pure internally are side-effect free

    @Purity(SideEffectFree)
    public int sef_0_0(){
        return 2 * nonFinalField;
    }

    @Purity(SideEffectFree)
    public static int sef_0_1(int a){
        return a + nonFinalStaticField;
    }

    // Methods depending on final fields which are side-effect free

    @Purity(SideEffectFree)
    public int sef_1_0(int[] arr){
        return arr[finalField];
    }

    @Purity(SideEffectFree)
    public static int sef_1_1(int[] arr){
        return arr[finalStaticField];
    }

    // Methods depending on final fields which are impure

    @Purity(Impure)
    public void impure_0_0(){
        nonFinalField = finalField / 2;
    }

    @Purity(Impure)
    public static void impure_0_1(int a){
        nonFinalStaticField = a % finalStaticField;
    }
}
