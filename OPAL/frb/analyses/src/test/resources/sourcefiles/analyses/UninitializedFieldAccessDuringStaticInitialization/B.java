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
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * Subclass in the A->B->C class hierarchy.
 * 
 * @author Daniel Klauer
 */
public class B extends A {

    static int B1 = 42;
    static int B3 = 42;
    static int B21 = 42;

    static int B_getA23() {
        return A23;
    }

    static int B24 = 42;

    static int B_getB24() {
        return B24;
    }

    static int B_getC25() {
        return C.C25;
    }

    static int B27 = 42;
    static int B31 = 42;

    static int B_getA32() {
        return A32;
    }

    static int B33 = 42;

    static int B_getB33() {
        return B33;
    }

    static int B_getA34_viaA() {
        return A_getA34();
    }

    static int B35 = 42;

    static int B_getB35_viaA() {
        return A_getB35();
    }

    static int B_getA36() {
        return A36;
    }

    static int B_getA36_viaB() {
        return B_getA36();
    }

    static int B37 = 42;

    static int B_getB37() {
        return B37;
    }

    static int B_getB37_viaB() {
        return B_getB37();
    }

    static int samename1 = 42;
    static int samename2 = 42;

    static int B50 = 42;
    static int B60 = 42;
    static int B70 = 42;
    static int B71 = 42;
    static int B72 = 42;

    static void B_initB72() {
        B72 = 42;
    }
}
