package org.opalj.fpcf.fixtures.escape.interface_test;

import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.fpcf.properties.escape.EscapeInCallee;
import org.opalj.fpcf.properties.escape.EscapeViaStaticField;

public class Main {
    public static Object global;

    public static void main(String[] args) {
        Object o1 = new
                @EscapeInCallee(
                        value = "C is not extensible and does nothing",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        A a1 = new C();
        a1.foo(o1);
        a1.bar(o1);

        Object o2 = new
                @EscapeViaStaticField(
                        value = "B#foo let it escape", analyses = InterProceduralEscapeAnalysis.class)
                        Object();
        Object o3 = new
                @EscapeInCallee(
                        value = "B#bar let it not escape",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        A a2 = new B();
        a2.foo(o2);
        a2.bar(o3);

        Object o4 = new
                @EscapeViaStaticField(
                        value = "the defualt impl. let it escape",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        a1.bazz(o4);
    }

    public static void callBarA(A a) {
        Object o = new
                @AtMostEscapeInCallee(
                        value = "default implementation let it escape, but is overridden",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        a.bar(o);
    }

    public static void callFooA(A a) {
        Object o = new
                @EscapeViaStaticField(
                        value = "B#foo let it escape",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        a.foo(o);
    }

    public static void callBazzB(B a) {
        Object o = new
                @EscapeViaStaticField(
                        value = "default implementation let it escape",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        a.bazz(o);
    }

    public static void callBarB(B b) {
        Object o = new
                @AtMostEscapeInCallee(
                        value = "super is not called, but class is extensible, but we optimize as it is extensible",
                        analyses = InterProceduralEscapeAnalysis.class
                ) Object();
        b.bar(o);
    }
}
