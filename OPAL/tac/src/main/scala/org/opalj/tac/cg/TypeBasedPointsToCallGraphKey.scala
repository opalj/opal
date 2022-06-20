/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.SimpleContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedNewInstanceAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedUnsafePointsToAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object TypeBasedPointsToCallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey, SimpleContextsKey) ++:
            super.requirements(project)
    }

    override protected[cg] def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            TypeBasedPointsToAnalysisScheduler,
            TypeBasedConfiguredMethodsPointsToAnalysisScheduler,
            TypeBasedTamiFlexPointsToAnalysisScheduler,
            TypeBasedArraycopyPointsToAnalysisScheduler,
            TypeBasedUnsafePointsToAnalysisScheduler,
            TypeBasedNewInstanceAnalysisScheduler
        )
    }

    override def getTypeIterator(project: SomeProject): TypeIterator =
        new TypeIterator(project) with TypesBasedPointsToTypeIterator with SimpleContextProvider

}
