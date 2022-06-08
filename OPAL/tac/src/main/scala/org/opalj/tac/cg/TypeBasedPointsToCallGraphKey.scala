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
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeProvider
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
    ): Iterable[FPCFAnalysisScheduler] = {
        List(
            TypeBasedPointsToAnalysisScheduler,
            TypeBasedConfiguredMethodsPointsToAnalysisScheduler,
            TypeBasedTamiFlexPointsToAnalysisScheduler,
            TypeBasedArraycopyPointsToAnalysisScheduler,
            TypeBasedUnsafePointsToAnalysisScheduler,
            TypeBasedNewInstanceAnalysisScheduler
        )
    }

    override def getTypeProvider(project: SomeProject): TypeProvider =
        new TypeProvider(project) with TypesBasedPointsToTypeProvider with SimpleContextProvider

}
