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
package org.opalj.fpcf.fixtures.purity;

import org.opalj.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.analyses.L1PurityAnalysis;
import org.opalj.fpcf.analyses.L2PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;

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

    @Pure(value = "Uses final field", analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
    public int getScaledFinalField() {
        return 2 * finalField;
    }

    @Pure(value = "Uses effectively final field",
            eps = @EP(cf = PrimitiveTypes.class, field = "effectivelyFinalField",
                    pk = "FieldMutability", p = "EffectivelyFinalField"),
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
    public int getScaledEffectivelyFinalField() {
        return effectivelyFinalField / 2;
    }

    @Impure(value = "Uses non-final field", analyses = L0PurityAnalysis.class)
    public int getNegatedNonFinalField() {
        return -nonFinalField;
    }

    @Impure(value = "Uses non-final static field", analyses = L0PurityAnalysis.class)
    public static int getNonFinalStaticField() {
        return nonFinalStaticField;
    }

    @ExternallyPure("Modifies instance field")
    @Impure(value = "Modifies instance field",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public void setNonFinalField(int newValue) {
        nonFinalField = newValue;
    }

    @ContextuallyPure("Only modifies field of parameter")
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

    @Impure(value = "Uses non-final field", analyses = L0PurityAnalysis.class)
    public int sef_0_0() {
        return 2 * nonFinalField;
    }

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

    @ExternallyPure("Modifies instance field")
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

    @Impure(value = "Analysis does not support preloaded purity values",
            analyses = L0PurityAnalysis.class)
    public long getCurrentTime(){
        return System.nanoTime();
    }
}
