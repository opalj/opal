/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedFieldsKey
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet1
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

class AllocationSiteBasedPointsToAnalysis private[analyses] (
    final val project: SomeProject
) extends AbstractPointsToAnalysis with AllocationSiteBasedAnalysis

object AllocationSiteBasedPointsToAnalysisScheduler extends AbstractPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => AllocationSiteBasedPointsToAnalysis =
        new AllocationSiteBasedPointsToAnalysis(_)

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        val declaredFields = p.get(DeclaredFieldsKey)
        val initialFields = p.get(InitialInstantiatedFieldsKey)

        val dummyAllocationSitesPerType: Map[ReferenceType, Long] = initialFields
            .flatMap(_._2)
            .toSet
            .map { refType: ReferenceType => (refType, allocationSiteToLong(NoContext, 0, refType)) }
            .toMap

        initialFields.foreach { case (field, types) =>
            val fieldPointsToSet =
                types.foldLeft[AllocationSitePointsToSet](NoAllocationSites) { case (result, currType) =>
                    result.included(AllocationSitePointsToSet1(dummyAllocationSitesPerType(currType), currType))
                }
            val declaredField = declaredFields(field)

            ps.preInitialize(declaredField, AllocationSitePointsToSet.key) {
                case _: EPK[_, _] => InterimEUBP(declaredField, fieldPointsToSet)
                case eps          => throw new IllegalStateException(s"unexpected property: $eps")
            }

        }

        super.init(p, ps)
    }
}
