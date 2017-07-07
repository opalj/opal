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
package tactest;

/**
 * Class with various complex method signatures.
 *
 * @author Michael Eichberg
 */
@SuppressWarnings("unused") public class MethodSignatures {

    static void empty() {
        ;
    }

    static void sTakeInt(int i) {
        ;
    }

    static double sTakeDoubleInt(double d, int i) {
        return d + i;
    }

    static double sTakeDoubleDouble(double d1, double d2) {
        return d1 + d2;
    }

    static void sTakeDoubleIntDouble(double d1, int i, double d2) {
        ;
    }

    static void sTakeDoubleDoubleInt(double d1, double d2, int i) {
        ;
    }

    static void sTakeIntDoubleDouble(int i, double d1, double d2) {
        ;
    }

    void iTakeInt(int i) {
        ;
    }

    double iTakeDoubleInt(double d, int i) {
        iTakeInt(i);
        return d + i;
    }

    void iTakeDoubleDouble(double d1, double d2) {
        ;
    }

    void iTakeDoubleIntDouble(double d1, int i, double d2) {
        ;
    }

    void iTakeDoubleDoubleInt(double d1, double d2, int i) {
        ;
    }

    void iTakeIntDoubleDouble(int i, double d1, double d2) {
        ;
    }
}
