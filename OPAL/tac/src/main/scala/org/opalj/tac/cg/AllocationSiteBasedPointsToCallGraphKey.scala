/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.jdk.CollectionConverters.*

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.log.LogContext
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator

trait AllocationSiteBasedPointsToCallGraphKey extends PointsToCallGraphKey {

    override protected def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnalysesRegistry here
        super.registeredAnalyses(project) ++ config.getStringList(
            "org.opalj.tac.cg.AllocationSites.modules"
        ).asScala.flatMap { moduleName =>
            resolveAnalysisRunner(if (moduleName.contains('.')) moduleName else getModuleFQN(moduleName))
        }
    }

}

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToCallGraphKey extends AllocationSiteBasedPointsToCallGraphKey {

    override val pointsToType: String = "AllocationSiteBased"
    override val contextKey: ProjectInformationKey[SimpleContexts, Nothing] = SimpleContextsKey

    override def getTypeIterator(project: SomeProject): AllocationSitesPointsToTypeIterator =
        new AllocationSitesPointsToTypeIterator(project)
}
