/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import scala.collection.mutable

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context

class AllocationSiteBasedAliasSet extends AliasSetLike[AllocationSite, AllocationSiteBasedAliasSet] {

    private var _pointsTo: mutable.Set[AllocationSite] = mutable.Set.empty[AllocationSite]

    def addPointsTo(context: Context, pc: PC): Unit = {
        addPointsTo((context, pc))
    }

    def pointsTo(context: Context, pc: PC): Boolean = _pointsToAny || pointsTo((context, pc))

    def allPointsTo: mutable.Set[AllocationSite] = _pointsTo

    def intersection(other: AllocationSiteBasedAliasSet): AllocationSiteBasedAliasSet = {
        val intersection = new AllocationSiteBasedAliasSet

        // we could theoretically optimize this by stopping once two intersecting elements have been found
        // because more than two intersecting elements won't change the behaviour of an alias analysis
        intersection._pointsTo = _pointsTo.intersect(other._pointsTo)
        intersection._pointsToAny = _pointsToAny || other._pointsToAny

        intersection
    }

}
