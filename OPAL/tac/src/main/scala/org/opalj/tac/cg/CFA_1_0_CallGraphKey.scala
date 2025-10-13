/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.CallStringContextProvider
import org.opalj.br.fpcf.properties.CallStringContexts
import org.opalj.br.fpcf.properties.CallStringContextsKey
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeIterator

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_0_CallGraphKey extends PointsToCallGraphKey {

    override val pointsToType: String = "TypeBased"
    override val contextKey: ProjectInformationKey[CallStringContexts, Nothing] = CallStringContextsKey

    override protected[cg] def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        TypeBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }

    override def getTypeIterator(project: SomeProject): TypeIterator =
        new TypeIterator(project) with TypesBasedPointsToTypeIterator with CallStringContextProvider { val k = 1 }
}
