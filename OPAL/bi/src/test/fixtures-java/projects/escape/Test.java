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
package escape;

import escape.ClassWithFields;

import annotations.target.EscapeKeys;
import annotations.target.EscapeProperty;


public class Test {


    public static Object returnEscape() {
        return new @EscapeProperty(EscapeKeys.GlobalEscape) Object();
    }

    public static Object multipleEscapes(ClassWithFields param) {
        ClassWithFields.global = new @EscapeProperty(EscapeKeys.GlobalEscape) Object();
        if (param == null) {
            throw new @EscapeProperty(EscapeKeys.GlobalEscape) RuntimeException();
        }
        param.f = new @EscapeProperty(EscapeKeys.GlobalEscape) Object();
        Object local = new @EscapeProperty(EscapeKeys.NoEscape) Object();
        Object noLocal = new @EscapeProperty(EscapeKeys.GlobalEscape) Object();
        if (local != null) {
            formalParamEscape(noLocal);
        }
        return new @EscapeProperty(EscapeKeys.GlobalEscape) Object();
    }

    public static void globalFieldEscape() {
        ClassWithFields.global = new
                @EscapeProperty(EscapeKeys.GlobalEscape) Object();
    }

    public static void parameterEscape(ClassWithFields param) {
        param.f = new
                @EscapeProperty(EscapeKeys.GlobalEscape) Object();
    }

    public static void exceptionEscape() {
        throw new @EscapeProperty(EscapeKeys.GlobalEscape) RuntimeException();
    }

    public static void staticMethodEscape() {
        formalParamEscape(new @EscapeProperty(EscapeKeys.GlobalEscape) Object());
    }

    public static void localNoEscape(boolean b) {
        Object x = new @EscapeProperty(EscapeKeys.NoEscape) Object();
        if (b)
            x = null;
        else
            x = null;
        ClassWithFields.global = x;

    }

    public static int simpleLocalNoEscape(boolean b) {
        Object x = new @EscapeProperty(EscapeKeys.NoEscape) Object();
        if (b)
            x = null;
        if (x != null) {
            return 1;
        }
        return 0;
    }

    public static int nonObjectLocalNoEscape(boolean b) {
        ClassWithFields x = new @EscapeProperty(EscapeKeys.NoEscape) ClassWithFields();
        if (b)
            x = null;
        if (x != null) {
            return 1;
        }
        return 0;
    }

    public static void constructorEscape() {
        new @EscapeProperty(EscapeKeys.GlobalEscape) ClassWithFields(1);
    }

    public static void formalParamEscape(@EscapeProperty(EscapeKeys.GlobalEscape) Object param) {
        ClassWithFields.global = param;
    }
}