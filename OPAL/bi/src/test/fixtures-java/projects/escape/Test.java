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

import annotations.escape.Escapes;

import static annotations.escape.EscapeKeys.*;

/**
 * @author Florian Kuebler
 */
public class Test {

    public static Object returnEscape() {
        return new @Escapes(ViaReturn) Object();
    }

    public static Object multipleEscapes(ClassWithFields param) {
        ClassWithFields.global = new @Escapes(ViaStaticField) Object();
        if (param == null) {
            throw new @Escapes(ViaReturn) @Escapes(value = MaybeNo, algorithms = {
                    "SimpleEscapeAnalysis", "InterproceduralEscapeAnalysis" }) RuntimeException();
        }
        param.f = new @Escapes(ViaParameter) @Escapes(value = MaybeMethod, algorithms = "SimpleEscapeAnalysis") Object();
        Object local = new @Escapes(No) Object();
        Object noLocal = new @Escapes(ViaStaticField) @Escapes(value = MaybeArg, algorithms = "SimpleEscapeAnalysis") Object();
        if (local != null) {
            formalParamEscape(noLocal);
        }
        return new @Escapes(ViaReturn) Object();
    }

    public static void globalFieldEscape() {
        ClassWithFields.global = new @Escapes(ViaStaticField) Object();
    }

    public static void parameterFieldGlobalEscape(ClassWithFields param) {
        param.f = new @Escapes(ViaHeapObject) @Escapes(value = MaybeMethod, algorithms = "SimpleEscapeAnalysis") Object();
        ClassWithFields.global = param;
    }

    public static void instanceFieldLoop() {
        ClassWithFields x = new @Escapes(ViaStaticField) ClassWithFields();
        x.g = x;
        while (true) {
            if (System.currentTimeMillis() == 12345678)
                break;
            x = x.g;
        }
        ClassWithFields.global = x;
    }

    public static void instanceFieldLoop2() {
        ClassWithFields x = new @Escapes(No) ClassWithFields();
        ClassWithFields y = x;
        while (true) {
            if (System.currentTimeMillis() == 123456789)
                break;
            y.g = new @Escapes(ViaStaticField) ClassWithFields();
            y = y.g;
        }
        ClassWithFields.global = x.g;
    }

    public static void instanceFieldFlowNoEscape() {
        ClassWithFields x = new @Escapes(Arg) ClassWithFields();
        formalParamNoEscape(x);
        x.f = new @Escapes(No) Object();
    }

    public static void instanceFieldAlias() {
        ClassWithFields x = new @Escapes(No) ClassWithFields();
        ClassWithFields y = x;
        y.f = new @Escapes(ViaStaticField) Object();
        ClassWithFields.global = x.f;
    }

    public static void instanceFieldNoEscape() {
        ClassWithFields c = new ClassWithFields();
        c.f = new @Escapes(No) Object();
    }

    public static void arrayEscape(Object[] param) {
        if (param.length > 0)
            param[0] = new @Escapes(ViaParameter) @Escapes(value = MaybeMethod, algorithms = "SimpleEscapeAnalysis") Object();
    }

    public static void parameterEscape(ClassWithFields param) {
        param.f = new @Escapes(ViaParameter) @Escapes(value = MaybeMethod, algorithms = "SimpleEscapeAnalysis") Object();
    }

    public static void exceptionEscape() {
        throw new @Escapes(ViaReturn) @Escapes(value = MaybeNo, algorithms = {
                "SimpleEscapeAnalysis", "InterproceduralEscapeAnalysis" }) RuntimeException();
    }

    public static void exceptionNoEscape() {
        try {
            throw new @Escapes(No) @Escapes(value = MaybeNo, algorithms = { "SimpleEscapeAnalysis",
                    "InterproceduralEscapeAnalysis" }) RuntimeException();
        } catch (Exception e) {
            System.out.println("catched the error");
        }

    }

    public static void staticMethodEscape() {
        formalParamEscape(
                new @Escapes(ViaStaticField) @Escapes(value = MaybeArg, algorithms = "SimpleEscapeAnalysis") Object());
    }

    public void virtualMethodEscape() {
        formalNonStaticParamEscape(
                new @Escapes(ViaStaticField) @Escapes(value = MaybeArg, algorithms = "SimpleEscapeAnalysis") Object());
    }

    public void virtualMethodInExprStmtEscape() {
        formalNonStaticParamEscapeWithReturn(
                new @Escapes(ViaStaticField) @Escapes(value = MaybeArg, algorithms = "SimpleEscapeAnalysis") Object(),
                new @Escapes(Arg) @Escapes(value = MaybeArg, algorithms = { "SimpleEscapeAnalysis",
                        "InterproceduralEscapeAnalysis" }) ClassWithFields());
    }

    public static void localNoEscape(boolean b) {
        Object x = new @Escapes(No) Object();
        if (b)
            x = null;
        else
            x = null;
        ClassWithFields.global = x;

    }

    public static int simpleLocalNoEscape(boolean b) {
        Object x = new @Escapes(No) Object();
        if (b)
            x = null;
        if (x != null) {
            return 1;
        }
        return 0;
    }

    public static int nonObjectLocalNoEscape(boolean b) {
        ClassWithFields x = new @Escapes(No) ClassWithFields();
        if (b)
            x = null;
        if (x != null) {
            return 1;
        }
        return 0;
    }

    public static void constructorEscape() {
        new @Escapes(ViaStaticField) ClassWithFields(1);
    }

    public static void formalParamEscape(
            @Escapes(ViaStaticField) @Escapes(value = MaybeNo, algorithms = "SimpleEscapeAnalysis") Object param) {
        ClassWithFields.global = param;
    }

    public static void formalParamNoEscape(
            @Escapes(No) @Escapes(value = MaybeNo, algorithms = "SimpleEscapeAnalysis") Object param) {
        if (param == null) {
            System.out.println("null");
        }
    }

    public static void argEscape() {
        formalParamNoEscape(
                new @Escapes(Arg) @Escapes(value = MaybeArg, algorithms = "SimpleEscapeAnalysis") Object());
    }

    public void formalNonStaticParamEscape(
            @Escapes(ViaStaticField) @Escapes(value = MaybeNo, algorithms = "SimpleEscapeAnalysis") Object param) {
        ClassWithFields.global = param;
    }

    public ClassWithFields formalNonStaticParamEscapeWithReturn(
            @Escapes(ViaStaticField) @Escapes(value = MaybeNo, algorithms = "SimpleEscapeAnalysis") Object p1,
            @Escapes(ViaReturn) @Escapes(value = MaybeMethod, algorithms = "InterproceduralEscapeAnalysis") @Escapes(value = MaybeNo, algorithms = "SimpleEscapeAnalysis") ClassWithFields p2) {
        p2.f = new Object();
        ClassWithFields.global = p1;
        return p2;
    }

    public int castEscape(Object param) {
        if (param == null) {
            param = new @Escapes(No) ClassWithFields();
        }
        if (!(param instanceof ClassWithFields))
            throw new IllegalArgumentException("Unsupported type!");

        Object o = ((ClassWithFields) param).f;

        if (o == null) {
            return -1;
        }
        return 1;
    }

    public static synchronized void noEscapeStaticFieldWrite() {
        ClassWithFields.global = new @Escapes(No) @Escapes(value = ViaStaticField, algorithms = {
                "SimpleEscapeAnalysis", "InterproceduralEscapeAnalysis" }) Object();
        ClassWithFields.global = null;
    }

    public static void globalEscapeGreaterArgEscape() {
        Object o = new @Escapes(ViaStaticField) Object();
        formalParamNoEscape(o);
        ClassWithFields.global = o;
    }
}
