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
package ai.domain;

/**
 * A class that does perform conditional math operations.
 * 
 * @author Michael Eichberg
 */
public class ConditionalMath {

    static int m1(int p) {
        int i;

        if (p == 1)
            i = 7;
        else
            i = 5;

        int j;
        if (i <= 5)
            j = i;
        else
            j = Integer.MAX_VALUE;

        return j + j;
    }

    static int m2(int p1, int p2) {
        if (p1 > 0)
            return m2(p1 - 1, p2) + 1;
        else if (p2 == 0)
            return 1;
        else
            return m2(p2, p2 - 1) + 1;
    }

    static int m3(int p) {
        try {
            return 100 / p;
        } catch (ArithmeticException ae) {
            return Integer.MIN_VALUE + p;
        }
    }

    static int max5(int l) {

        int i;
        if (l < 5) {
            i = l;
        } else {
            i = 5;
        }
        return i;
    }

    static int aliases(int l) {
        int p = 0;
        int i;
        if (l < 5) {
            i = l;
        } else {
            p = l;
            if (p < 5) {
                // this line should never be reached... p is an alias of l and l larger or
                // equal to 5
                throw new UnknownError();
            } else {
                i = -5;
            }
        }
        return i;
    }

    public static void main(String[] args) {
        System.out.println(m1(100));
        System.out.println(m2(1, 1));
        System.out.println(m2(2, 2));
        System.out.println(m2(3, 3));
        System.out.println(m3(100));
    }

}
