/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;
import org.opalj.tac.fpcf.analyses.alias.pointsto.TypePointsToBasedAliasAnalysis;

public class ReturnValueAlias {

    @NoAliasLine(reason = "no Alias with local variable", lineNumber = 17, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public static Object noAliasWithLocal() {
        Object o1 = new Object();

        o1.hashCode();

        return new Object();
    }

    @MayAliasLine(reason = "mayAlias with local variable", lineNumber = 31)
    public static Object mayAliasWithLocal1() {
        Object o1 = new Object();
        Object o2 = new Object();

        if (Math.random() > 0.5) {
            o2 = o1;
        }

        o2.hashCode();

        return o2;
    }

    @MayAliasLine(reason = "mayAlias with local variable", lineNumber = 40)
    public static Object mayAliasWithLocal2() {
        Object o1 = new Object();

        o1.hashCode();

        return o1;
    }

    @NoAlias(reason = "noAlias with parameter", id = 0, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, TypePointsToBasedAliasAnalysis.class})
    public static Object noAliasWithParam(
            @NoAlias(reason = "noAlias with parameter", id = 0, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, TypePointsToBasedAliasAnalysis.class})
            Object a) {
        Object o1 = new Object();
        return o1;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 1)
    public static Object mayAliasWithParam1(
            @MayAlias(reason = "mayAlias with parameter", id = 1)
            Object a) {
        return a;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 2)
    public static Object mayAliasWithParam2(
            @MayAlias(reason = "mayAlias with parameter", id = 2)
            Object a) {
        return a;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 3)
    public static Object mayAliasWithParam3(
            @MayAlias(reason = "mayAlias with parameter", id = 3)
            Object a) {

        Object o1 = new Object();

        if (Math.random() > 0.5) {
            o1 = a;
        }

        return o1;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 4)
    public static Object mayAliasWithParam4(
            @MayAlias(reason = "mayAlias with parameter", id = 4)
            Object a) {

        Object o1 = new Object();

        if (Math.random() > 0.5) {
            return o1;
        }

        return a;
    }

    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();

        mayAliasWithParam1(o1);

        mayAliasWithParam2(o1);
        mayAliasWithParam2(o2);

        mayAliasWithParam3(o1);

        mayAliasWithParam4(o1);
    }

}
