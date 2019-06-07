/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong

class AllocationSiteBasedPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis[AllocationSitePointsToSet] {
    override def createPointsToSet(pc: Int, declaredMethod: DeclaredMethod, allocatedType: ObjectType): AllocationSitePointsToSet = {
        AllocationSitePointsToSet(
            LongTrieSet(allocationSiteToLong(declaredMethod, pc)),
            UIDSet(allocatedType),
            List(allocatedType)
        )
    }

    override protected val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSet] = {
        AllocationSitePointsToSet.key
    }

    override protected def emptyPointsToSet: AllocationSitePointsToSet = {
        NoAllocationSites
    }
}
