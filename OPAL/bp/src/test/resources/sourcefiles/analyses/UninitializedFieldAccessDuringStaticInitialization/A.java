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
 * Superclass in the A->B->C class hierarchy. All tests are based on the static
 * initializer here in the superclass.
 * 
 * @author Daniel Klauer
 */
public class A {

    // Direct access to subclass'es static field, should be reported. In the source code,
    // this access appears in static initialization outside the `static { }` block, but
    // when compiled it will also appear in `<clinit>`, which is what the analysis
    // analyzes.
    static int A1 = B.B1; // uninitialized
    static int A2 = C.C2; // uninitialized

    static int A20 = 42;

    static int A_getA20() {
        return A20;
    }

    static int A_getB21() {
        return B.B21;
    }

    static int A_getC22() {
        return C.C22;
    }

    static int A23 = 42;
    static int A26 = 42;
    static int A30 = 42;

    static int A_getA30() {
        return A30;
    }

    static int A_getA30_viaA() {
        return A_getA30();
    }

    static int A_getB31() {
        return B.B31;
    }

    static int A_getB31_viaA() {
        return A_getB31();
    }

    static int A32 = 42;

    static int A_getA32_viaB() {
        return B.B_getA32();
    }

    static int A_getB33_viaB() {
        return B.B_getB33();
    }

    static int A34 = 42;

    static int A_getA34() {
        return A34;
    }

    static int A_getB35() {
        return B.B35;
    }

    static int A36 = 42;

    // Static fields in the class and its subclasses may have the same name. That should
    // not confuse the analysis though - it has to reference fields not only by name, but
    // also by their declaring class.
    static int samename1 = 42;
    static int samename2 = 42;

    static int A50 = 42;

    static {
        // Direct access
        System.out.println(B.B3); // uninitialized
        System.out.println(C.C4); // uninitialized

        // Various indirect accesses (1 level of indirection)
        System.out.println(A_getA20()); // ok
        System.out.println(A_getB21()); // uninitialized
        System.out.println(A_getC22()); // uninitialized
        System.out.println(B.B_getA23()); // ok
        System.out.println(B.B_getB24()); // uninitialized
        System.out.println(B.B_getC25()); // uninitialized
        System.out.println(C.C_getA26()); // ok
        System.out.println(C.C_getB27()); // uninitialized
        System.out.println(C.C_getC28()); // uninitialized

        // Access to uninitialized field through 2 levels of indirection
        System.out.println(A_getA30_viaA()); // ok
        System.out.println(A_getB31_viaA()); // uninitialized
        System.out.println(A_getA32_viaB()); // ok
        System.out.println(A_getB33_viaB()); // uninitialized
        System.out.println(B.B_getA34_viaA()); // ok
        System.out.println(B.B_getB35_viaA()); // uninitialized
        System.out.println(B.B_getA36_viaB()); // ok
        System.out.println(B.B_getB37_viaB()); // uninitialized

        // Accessing this class'es static field first, it's initialized already.
        // The following access to the subclass'es static field should still be detected
        // as uninitialized access (but would not be if the analysis only checked fields
        // based on their name).
        System.out.println(samename1); // ok
        System.out.println(B.samename1); // uninitialized

        // Accessing the subclass'es static field first, this is an uninitialized access.
        // The following access to this class'es static field should not be mis-detected
        // as uninitialized access (but would be if the analysis only checked fields
        // based on their name).
        System.out.println(B.samename2); // uninitialized
        System.out.println(samename2); // ok

        // Direct access but through context other than the field's declaringClass,
        // shouldn't confuse the analysis
        System.out.println(B.A50); // ok
        System.out.println(C.A50); // ok
        System.out.println(C.B50); // uninitialized

        // Second access to an uninitialized field. Only the first access should be
        // reported.
        System.out.println(B.B60); // uninitialized
        System.out.println(B.B60); // uninitialized, but ignored

        // Initializing a field manually prevents the uninitialized access
        B.B70 = 42;
        System.out.println(B.B70); // ok
        A_initB71();
        System.out.println(B.B71); // ok
        B.B_initB72();
        System.out.println(B.B72); // ok
    }

    // Inner class that also is a subclass
    static class InnerB extends A {

        static int InnerB40 = 42;
    }

    static int A40 = InnerB.InnerB40; // uninitialized

    static void A_initB71() {
        B.B71 = 42;
    }
}
