/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;
import org.opalj.tac.fpcf.analyses.alias.pointsto.TypePointsToBasedAliasAnalysis;

public class NullAlias {

    public static void main(String[] args) {
        paramIsAlwaysNull(null);
        paramMayBeNull(null);
        paramMayBeNull(new Object());
    }
    
    public static void paramIsAlwaysNull(
            @NoAliasLine(reason = "parameter is always null", lineNumber = 20, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, TypePointsToBasedAliasAnalysis.class})
            Object o) {
        o.hashCode();
    }

    public static void paramMayBeNull(
            @MayAliasLine(reason = "parameter may be null", lineNumber = 26)
            Object o) {
        o.hashCode();
    }

    @NoAliasLine(reason = "uVar is always null", lineNumber = 32, secondLineNumber = 32, analyses = {AllocationSitePointsToBasedAliasAnalysis.class, TypePointsToBasedAliasAnalysis.class})
    public static void UVarIsAlwaysNull() {
        Object o = null;
        o.hashCode();
    }

    @MayAliasLine(reason = "uVar may be null", lineNumber = 41, secondLineNumber = 41)
    public static void UVarMayBeNull() {
        Object o = null;
        if (Math.random() > 0.5) {
            o = new Object();
        }
        o.hashCode();
    }

}
