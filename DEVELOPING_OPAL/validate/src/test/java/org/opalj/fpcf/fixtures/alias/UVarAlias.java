/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.MustAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.IntraProceduralAliasAnalysis;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;

public class UVarAlias {

    @MustAliasLine(reason = "same local variable with single defSite without loop used",
            lineNumber = 18,
            secondLineNumber = 18, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mustAliasLocals() {
        Object o1 = new Object();

        o1.hashCode();
    }

    @MayAliasLine(reason = "same local variable with single defSite with loop used",
            lineNumber = 27,
            secondLineNumber = 27, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mayAliasLoop() {
        for (int i = 0; i < 10; i++) {
            Object o1 = new Object();
            o1.hashCode();
        }
    }

    @MustAliasLine(reason = "same local variable with single defSite with loop in front of defSite",
            lineNumber = 41,
            secondLineNumber = 41, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mustAliasLoopInFront() {
        for (int i = 0; i < 10; i++) {
            Object o1 = new Object();
        }

        Object o2 = new Object();

        o2.hashCode();
    }

    @MustAliasLine(reason = "same local variable with single defSite with loop behind defSite",
            lineNumber = 50,
            secondLineNumber = 50, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mustAliasLoopBehind() {
        Object o1 = new Object();

        o1.hashCode();

        for (int i = 0; i < 10; i++) {
            Object o2 = new Object();
        }
    }

    @MustAliasLine(reason = "same local variable with single defSite with loop behind defSite",
            lineNumber = 64,
            secondLineNumber = 64, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mustAliasLoopBehind2() {
        Object o1 = new Object();

        for (int i = 0; i < 10; i++) {
            o1.hashCode();
        }
    }

    @MayAliasLine(reason = "same local variable with single defSite with recursion",
            lineNumber = 72,
            secondLineNumber = 74, secondParameterIndex = 0)
    public static void mayAliasRecursion(Object a) {
        a.hashCode();
        a = new Object();
        mayAliasRecursion(a);
    }

    @MustAliasLine(reason = "same local variable with single defSite with irrelevant recursion",
            lineNumber = 82,
            secondLineNumber = 83, secondParameterIndex = 0, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void mustAliasRecursion(Object a) {
        a = new Object();
        a.hashCode();
        mustAliasRecursion(a);
    }

    @MayAliasLine(reason = "same local variable with single defSite in other method",
            lineNumber = 92,
            secondLineNumber = 92)
    public static void mayAliasSameVariableOtherMethod() {
        Object o1 = createNewObject();

        o1.hashCode();
    }

    @MayAliasLine(reason = "different local variable with single defSite in other method",
            lineNumber = 102,
            secondLineNumber = 103)
    public static void mayAliasDifferentVariableOtherMethod() {
        Object o1 = createNewObject();
        Object o2 = createNewObject();

        o1.hashCode();
        o2.hashCode();
    }

    public static Object createNewObject() {
        return new Object();
    }

    @NoAliasLine(reason = "no alias with local variables",
            lineNumber = 116,
            secondLineNumber = 117, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, IntraProceduralAliasAnalysis.class})
    public static void noAliasLocals() {
        Object o1 = new Object();
        Object o2 = new Object();
        o1.hashCode();
        o2.hashCode();
    }

    @MayAliasLine(reason = "may alias with local variables",
            lineNumber = 131,
            secondLineNumber = 132)
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
