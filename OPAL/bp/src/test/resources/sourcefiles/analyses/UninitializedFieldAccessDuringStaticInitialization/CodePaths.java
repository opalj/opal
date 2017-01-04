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
 * This test covers the analysis' ability to track the code-path-specific initialization
 * status of static fields.
 * 
 * @author Daniel Klauer
 */
public class CodePaths {

    public static int condition10 = 0;
    public static int condition11 = 0;
    public static int condition12 = 0;
    public static int condition20 = 0;
    public static int condition21 = 0;
    public static int condition22 = 0;
    public static int condition30 = 0;
    public static int condition31 = 0;
    public static int condition40 = 0;

    static void test11() {
        if (condition11 != 0) {
        } else {
            System.out.println(CodePathsSubclass.i11); // uninitialized
        }
    }

    static void test40() {
        if (condition40 != 0) {
            System.out.println(CodePathsSubclass.i40); // uninitialized
            return;
        }

        System.out.println(CodePathsSubclass.i40); // uninitialized
    }

    static {
        // Uninitialized access on only 1 of 2 code paths
        if (condition10 != 0) {
            System.out.println(CodePathsSubclass.i10); // uninitialized
        } else {
        }

        // same but in a subroutine
        test11();

        // Uninitialized accesses on both code paths
        if (condition12 != 0) {
            System.out.println(CodePathsSubclass.i12); // uninitialized
        } else {
            System.out.println(CodePathsSubclass.i12); // uninitialized
        }

        // Field initialized manually, but only on 1 of 2 code paths,
        // potentially causing an uninitialized access later
        if (condition20 != 0) {
            CodePathsSubclass.i20 = 1;
        }
        System.out.println(CodePathsSubclass.i20); // maybe uninitialized

        if (condition21 != 0) {
            CodePathsSubclass.i21 = 1;
        } else {
            CodePathsSubclass.i21 = 2;
        }
        System.out.println(CodePathsSubclass.i21); // ok

        if (condition22 != 0) {
            CodePathsSubclass.i22 = 1;
            System.out.println(CodePathsSubclass.i22); // ok
        }

        // Field initialized manually on one code path,
        // and accessed uninitialized on the other.
        if (condition30 != 0) {
            CodePathsSubclass.i30 = 1;
        } else {
            System.out.println(CodePathsSubclass.i30); // uninitialized
            // (if the analysis didn't track code paths, but just looked through
            // instructions one by one, this would probably not be detected as an
            // uninitialized access.
        }

        if (condition31 != 0) {
            System.out.println(CodePathsSubclass.i31); // uninitialized
        } else {
            CodePathsSubclass.i31 = 1;
        }

        test40();
    }
}

class CodePathsSubclass extends CodePaths {

    static int i10 = 42;
    static int i11 = 42;
    static int i12 = 42;
    static int i20 = 42;
    static int i21 = 42;
    static int i22 = 42;
    static int i30 = 42;
    static int i31 = 42;
    static int i40 = 42;
}
