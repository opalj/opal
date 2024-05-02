/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;

public class ParameterAlias {

    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();

        noAliasWithLocal(o1);

        noAliasWithParam(o1, o2);

        mayAliasWithLocal(o1);

        mayAliasWithParam1(o1, o2);
        mayAliasWithParam1(o1, o1);

        mayAliasWithParam2(o1, o1);


        ParameterAlias pa = new ParameterAlias();
        ParameterAlias pa2 = new ParameterAlias();

        pa.mayAliasThisParam();
        pa2.noAliasThisParam();

        pa.mayAliasThisParamTwoMethods1();
        pa.mayAliasThisParamTwoMethods2();

        pa.noAliasThisParamTwoMethods1();
        pa2.noAliasThisParamTwoMethods2();
    }

    public static void noAliasWithLocal(@NoAliasLine(reason = "noAlias with uVar", lineNumber = 44, analyses = {AllocationSitePointsToBasedAliasAnalysis.class}) Object o1) {

        Object o2 = new Object();
        o2.hashCode();
    }

    public static void noAliasWithParam(@NoAlias(reason = "noAlias with other parameter", id = 0, analyses = {AllocationSitePointsToBasedAliasAnalysis.class}) Object o1,
                                        @NoAlias(reason = "noAlias with other parameter", id = 0, analyses = {AllocationSitePointsToBasedAliasAnalysis.class}) Object o2) {

    }

    public static void mayAliasWithLocal(@MayAliasLine(reason = "mayAlias with uVar", lineNumber = 60) Object o1) {

        Object o2 = new Object();

        if (Math.random() > 0.5) {
            o2 = o1;
        }

        o2.hashCode();
    }

    public static void mayAliasWithParam1(@MayAlias(reason = "mayAlias with other parameter 1", id = 1) Object o1,
                                          @MayAlias(reason = "mayAlias with other parameter 1", id = 1) Object o2) {

    }

    public static void mayAliasWithParam2(@MayAlias(reason = "mayAlias with other parameter 2", id = 2) Object o1,
                                          @MayAlias(reason = "mayAlias with other parameter 2", id = 2) Object o2) {

    }

    @MayAliasLine(reason = "may alias with this parameter and invoked uVar", thisParameter = true,
            lineNumber = 31, methodName = "main")
    public void mayAliasThisParam() {}

    @NoAliasLine(reason = "no alias with this parameter and invoked uVar", thisParameter = true,
            lineNumber = 31, methodName = "main", analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void noAliasThisParam() {}

    @MayAlias(reason = "may alias with this parameter of two methods", thisParameter = true,
            id = 3)
    public void mayAliasThisParamTwoMethods1() {}

    @MayAlias(reason = "may alias with this parameter of two methods", thisParameter = true,
            id = 3)
    public void mayAliasThisParamTwoMethods2() {}

    @NoAlias(reason = "no alias with this parameter of two methods", thisParameter = true,
            id = 4, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void noAliasThisParamTwoMethods1() {}

    @NoAlias(reason = "no alias with this parameter of two methods", thisParameter = true,
            id = 4, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void noAliasThisParamTwoMethods2() {}

}
