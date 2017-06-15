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
import org.opalj.fpcf.test.annotations.EscapeKeys;
import org.opalj.fpcf.test.annotations.EscapeProperty;


public class Test {

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static Object returnEscape() {
        return new Object();
    }

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static void globalFieldEscape() {
        ClassWithFields.global = new Object();
    }

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static void parameterEscape(ClassWithFields param) {
        param.f = new Object();
    }

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static void exceptionEscape() {
        throw new RuntimeException();
    }

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static void staticMethodEscape() {
        bar(new Object());
    }

    @EscapeProperty(EscapeKeys.NoEscape)
    public static int simpleLocalNoEscape(boolean b) {
        Object x = new Object();
        if (b)
            x = null;
        if (x != null) {
            return 1;
        }
        return 0;
    }

    @EscapeProperty(EscapeKeys.NoEscape)
    public static void localNoEscape(boolean b) {
        Object x = new Object();
        if (b)
            x = null;
        else
            x = null;
        ClassWithFields.global = x;

    }

    @EscapeProperty(EscapeKeys.GlobalEscape)
    public static Object foo(ClassWithFields param) {
        ClassWithFields.global = new Object();
        if (param == null) {
            throw new RuntimeException();
        }
        param.f = new Object();
        Object local = new Object();
        Object noLocal = new Object();
        if (!local.equals(noLocal)) {
            bar(noLocal);
        }
        return new Object();

    }

    public static Object tac(Object param) {
        Object local1 = new Object();
        Object local2 = new Object();

        if (param != null) {
            local1 = local2;
        }

        Object local3 = local1;

        return local3;

    }

    public static void bar(Object param) {
        ClassWithFields.global = param;
    }

}