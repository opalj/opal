/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package performInvocations;

public class StaticCalls {

    static void doNothing() {
        // empty
    }

    static int returnConstantIntValue() {
        return 1;
    }

    static Object returnObject() {
        return new Object();
    }

    static void throwException() {
        throw new UnsupportedOperationException();
    }

    static long returnObjectOrthrowException() {
        long t = System.currentTimeMillis();
        if (t % 100 == 0)
            throw new UnsupportedOperationException();
        else
            return t;
    }

    static double div(int a, int b) {
        if (b == 0)
            return Double.NaN;
        else
            return a / b;
    }

    static int divBy4(int a) {
        return a / 4;
    }

    static int complexMult(int a, int b) {
        return (a + b) * (a / b);
    }

    static long performCalculation() {
        return (long) div(1212, 23423);
    }

    static long doStuff() {
        returnObject();
        div(returnConstantIntValue(), 0);
        return performCalculation();
    }

    static Double alwaysFail() {
        throwException();
        return new Double(Double.NaN);
    }

    static int callDivBy4() {
        return divBy4(100);
    }

    static int aCallChain(int i) {
        return i * callDivBy4();
    }

    static int aLongerCallChain() {
        return aCallChain(returnConstantIntValue() * 7);
    }

    static int callComplexMult() {
        return complexMult(10, returnConstantIntValue());
    }

    static void mayFail() {
        returnObjectOrthrowException();
    }

    static void throwMultipleExceptions(int i) throws java.lang.Throwable {
        switch (i) {
        case 0:
        case 1:
            throw new IllegalArgumentException();
        case 2:
            throw new NullPointerException();
        default:
            System.out.println("Ok");
        }

        Throwable e = null;
        if (System.currentTimeMillis() % 100 == 0)
            e = new UnsupportedOperationException();
        else
            e = new UnknownError();

        throw e;
    }

    static void simpleRecursion(boolean b) {
        if (b)
            simpleRecursion(false);
        else
            System.out.println("done");
    }

    static void endless() {
        System.out.println(System.currentTimeMillis());
        endless();
    }

    static int fak(int i) {
        if (i > 1)
            return i * fak(i - 1);
        else
            return 1;
    }
}
