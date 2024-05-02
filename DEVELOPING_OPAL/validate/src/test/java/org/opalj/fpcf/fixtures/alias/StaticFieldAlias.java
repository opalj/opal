/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;

public class StaticFieldAlias {

    @MayAliasLine(reason = "may alias with field and assigned uVar", lineNumber = 43, methodName = "reassignField")
    @NoAlias(reason = "no alias with field and parameter", id = 1, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    @NoAlias(reason = "no alias with field and return value", id = 3, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    @NoAliasLine(reason = "no alias with field and unrelated uVar", lineNumber = 56, methodName = "noAlias", analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    @MayAlias(reason = "may alias with field and return value", id = 2)
    @MayAliasLine(reason = "may alias with field and returned uVar", lineNumber = 67, methodName = "returnMayAliasField")
    @MayAlias(reason = "may alias with field and return value via parameter", id = 5)
    @MayAliasLine(reason = "may alias with field and returned uVar via parameter", lineNumber = 82, methodName = "parameterIsMayAliasField")
    @MayAlias(reason = "may alias with field and parameter", id = 6)
    public static Object mayAliasField = new Object();

    @MayAliasLine(reason = "may alias with field and assigned uVar", lineNumber = 61, methodName = "reassignNullField")
    public static Object nullField = null;

    @MayAlias(reason = "may alias with static final field and return value", id = 4)
    @MayAliasLine(reason = "may alias with static final field and returned uVar", lineNumber = 73, methodName = "returnStaticFinalField")
    @MayAlias(reason = "may alias with static final field and return value via parameter", id = 7)
    @MayAliasLine(reason = "may alias with static final field and returned uVar via parameter", lineNumber = 89, methodName = "parameterIsStaticFinalField")
    @MayAlias(reason = "may alias with static final field and parameter", id = 8)
    public static Object staticFinalField = new Object();

    public static void main(String[] args) {
        noAlias(new Object());
        noAlias(staticFinalField);

        parameterIsMayAliasField(mayAliasField);
        parameterIsStaticFinalField(staticFinalField);
    }

    public static void reassignField() {
        Object o = new Object();
        mayAliasField = o;
    }

    @NoAlias(reason = "no alias with field and return value", id = 3, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public static Object noAlias(
            @NoAlias(reason = "no alias with field and parameter", id = 1, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
            Object a) {
        Object o = new Object();

        if (Math.random() > 0.5) {
            o = a;
        }

        return o;
    }

    public static void reassignNullField() {
        Object o = new Object();
        nullField = o;
    }

    @MayAlias(reason = "may alias with field and return value", id = 2)
    public static Object returnMayAliasField() {
        Object o = mayAliasField;
        return o;
    }

    @MayAlias(reason = "may alias with static final field and return value", id = 4)
    public static Object returnStaticFinalField() {
        Object o = staticFinalField;
        return o;
    }

    @MayAlias(reason = "may alias with field and return value via parameter", id = 5)
    @MayAliasLine(reason = "may alias with return value and uVar via parameter and field", lineNumber = 43, methodName = "reassignField")
    public static Object parameterIsMayAliasField(
            @MayAlias(reason = "may alias with field and parameter", id = 6)
            @MayAliasLine(reason = "may alias with parameter and UVar via field", lineNumber = 43, methodName = "reassignField")
            Object a) {
        return a;
    }

    @MayAlias(reason = "may alias with static final field and return value via parameter", id = 7)
    public static Object parameterIsStaticFinalField(
            @MayAlias(reason = "may alias with static final field and parameter", id = 8)
            Object a) {
        return a;
    }

}
