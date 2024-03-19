/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.MustAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class UVarAlias {

    @AliasMethodID(id = 0, clazz = UVarAlias.class)
    @MustAliasLine(reason = "same local variable with single defSite without loop used",
            lineNumber = 19, methodID = 0,
            secondLineNumber = 20, secondMethodID = 0,
            clazz = UVarAlias.class)
    public static void mustAliasLocals() {
        Object o1 = new Object();

        o1.hashCode();
        o1.hashCode();
    }

    @AliasMethodID(id = 1, clazz = UVarAlias.class)
    @MayAliasLine(reason = "same local variable with single defSite with loop used",
            lineNumber = 31, methodID = 1,
            secondLineNumber = 31, secondMethodID = 1,
            clazz = UVarAlias.class)
    public static void mayAliasLoop() {
        for (int i = 0; i < 10; i++) {
            Object o1 = new Object();
            o1.hashCode();
        }
    }

    @AliasMethodID(id = 2, clazz = UVarAlias.class)
    @MustAliasLine(reason = "same local variable with single defSite with loop in front of defSite",
            lineNumber = 47, methodID = 2,
            secondLineNumber = 48, secondMethodID = 2,
            clazz = UVarAlias.class)
    public static void mustAliasLoopInFront() {
        for (int i = 0; i < 10; i++) {
            Object o1 = new Object();
        }

        Object o2 = new Object();

        o2.hashCode();
        o2.hashCode();
    }

    @AliasMethodID(id = 3, clazz = UVarAlias.class)
    @MustAliasLine(reason = "same local variable with single defSite with loop behind defSite",
            lineNumber = 59, methodID = 3,
            secondLineNumber = 60, secondMethodID = 3,
            clazz = UVarAlias.class)
    public static void mustAliasLoopBehind() {
        Object o1 = new Object();

        o1.hashCode();
        o1.hashCode();

        for (int i = 0; i < 10; i++) {
            Object o2 = new Object();
        }
    }

    @AliasMethodID(id = 4, clazz = UVarAlias.class)
    @MustAliasLine(reason = "same local variable with single defSite with loop behind defSite",
            lineNumber = 76, methodID = 4,
            secondLineNumber = 76, secondMethodID = 4,
            clazz = UVarAlias.class)
    public static void mustAliasLoopBehind2() {
        Object o1 = new Object();

        for (int i = 0; i < 10; i++) {
            o1.hashCode();
        }
    }

    @AliasMethodID(id = 5, clazz = UVarAlias.class)
    @MayAliasLine(reason = "same local variable with single defSite with recursion",
            lineNumber = 86, methodID = 5,
            secondLineNumber = 88, secondMethodID = 5, secondParameterIndex = 0,
            clazz = UVarAlias.class)
    public static void mayAliasRecursion(Object a) {
        a.hashCode();
        a = new Object();
        mayAliasRecursion(a);
    }

    @AliasMethodID(id = 6, clazz = UVarAlias.class)
    @MustAliasLine(reason = "same local variable with single defSite with irrelevant recursion",
            lineNumber = 98, methodID = 6,
            secondLineNumber = 99, secondMethodID = 6, secondParameterIndex = 0,
            clazz = UVarAlias.class)
    public static void mustAliasRecursion(Object a) {
        a = new Object();
        a.hashCode();
        mustAliasRecursion(a);
    }

    @AliasMethodID(id = 7, clazz = UVarAlias.class)
    @MayAliasLine(reason = "same local variable with single defSite in other method",
            lineNumber = 110, methodID = 7,
            secondLineNumber = 111, secondMethodID = 7,
            clazz = UVarAlias.class)
    public static void mayAliasSameVariableOtherMethod() {
        Object o1 = createNewObject();

        o1.hashCode();
        o1.hashCode();
    }

    @AliasMethodID(id = 8, clazz = UVarAlias.class)
    @MayAliasLine(reason = "different local variable with single defSite in other method",
            lineNumber = 123, methodID = 8,
            secondLineNumber = 124, secondMethodID = 8,
            clazz = UVarAlias.class)
    public static void mayAliasDifferentVariableOtherMethod() {
        Object o1 = createNewObject();
        Object o2 = createNewObject();

        o1.hashCode();
        o2.hashCode();
    }

    public static Object createNewObject() {
        return new Object();
    }

    @AliasMethodID(id = 9, clazz = UVarAlias.class)
    @NoAliasLine(reason = "no alias with local variables",
            lineNumber = 139, methodID = 9,
            secondLineNumber = 140, secondMethodID = 9,
            clazz = UVarAlias.class)
    public static void noAliasLocals() {
        Object o1 = new Object();
        Object o2 = new Object();
        o1.hashCode();
        o2.hashCode();
    }

    @AliasMethodID(id = 10, clazz = UVarAlias.class)
    @MayAliasLine(reason = "may alias with local variables",
            lineNumber = 156, methodID = 10,
            secondLineNumber = 157, secondMethodID = 10,
            clazz = UVarAlias.class)
    public static void mayAliasLocals() {
        Object o1 = new Object();
        Object o2 = new Object();

        if (Math.random() > 0.5) {
            o1 = o2;
        }

        o1.hashCode();
        o2.hashCode();
    }
}
