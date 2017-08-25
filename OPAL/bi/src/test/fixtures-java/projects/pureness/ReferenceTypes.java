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
 * Collection of simple test methods using object and array types for purity analyses that can be
 * analyzed intraprocedurally.
 *
 * @author Dominik Helm
 *
 */
class ReferenceTypes {

    // Fields with different attributes for use in test methods

    static final Object staticFinalObj = new Object();

    private static Object staticEffectivelyFinalObj = new Object();

    private static Object staticNonFinalObj = new Object();

    private final int[] nonConstFinalArr = new int[] { 1, 2, 3 };

    private int[] constEffectivelyFinalArr = new int[] { 2, 3, 4 };

    public int[] nonFinalArr = new int[] { 5, 6, 7 };

    // Using (effectively) final static fields is pure
    // Using non final static fields is side-effect free
    // Setting static fields is impure

    @Purity(Pure)
    public static Object getStaticFinalObj(){
        return staticFinalObj;
    }

    @Purity(Pure)
    public static Object getStaticEffectivelyFinalObj(){
        return staticEffectivelyFinalObj;
    }

    @Purity(SideEffectFree)
    public static Object getStaticNonFinalObj(){
        return staticNonFinalObj;
    }

    @Purity(Impure)
    public static void setStaticObj(Object newObject){
        staticNonFinalObj = newObject;
    }

    // Using the array-length is pure, as it is immutable
    // Reading array entries is side-effect free
    // (not pure even for parameters because of concurrent modification)
    // Writing array entries is impure
    // Reading and writing entries of fresh arrays is pure, though
    // Array access may throw IndexOutOfBounds exceptions, but this is pure

    @Purity(Pure)
    public int getFinalArrLength(){
        return nonConstFinalArr.length;
    }

    @Purity(Pure)
    public int getEffectivelyFinalArrLength(){
        return constEffectivelyFinalArr.length;
    }

    @Purity(SideEffectFree)
    public int getNonFinalArrLength() { return nonFinalArr.length; }

    @Purity(Pure)
    public static int getArrLengthStatic(int[] arr){
        return arr.length;
    }

    @Purity(SideEffectFree)
    public int getArrayEntries(int index){
        return nonConstFinalArr[index] + nonFinalArr[index];
    }

    @Purity(Pure)
    @Purity(value=SideEffectFree, algorithms="MethodPurityAnalysis") // Doesn't recognize const-ness
    public int getConstArrayEntry(int index) { return constEffectivelyFinalArr[index]; }

    @Purity(SideEffectFree)
    public static int getArrayEntryStatic(int index, int[] arr){
        return arr[index];
    }

    @Purity(Impure)
    public void setArrayEntry(int index, int value){
        nonConstFinalArr[index] = value;
    }

    @Purity(Impure)
    public static void setArrayEntryStatic(int[] arr, int index, int value){
        arr[index] = value;
    }

    @Purity(Pure)
    @Purity(value=Impure, algorithms="MethodPurityAnalysis") // Doesn't recognize freshness
    public static int getFreshArrayEntry(int index){
        int[] arr = new int[] { 1, 2, 3 };
        return arr[index];
    }

    @Purity(Pure)
    @Purity(value=Impure, algorithms="MethodPurityAnalysis") // Doesn't recognize freshness
    public static int[] setFreshArrayEntry(int index, int value){
        int[] arr = new int[] { 1, 2, 3 };
        arr[index] = value;
        return arr;
    }

    // Synchronization is impure (unless done on a fresh, non-escaping object)

    @Purity(Impure)
    private int syncMethod(){
        int result;
        synchronized (this) {
            result = 5;
        }
        return result;
    }

}
