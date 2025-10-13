/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.jdk.CollectionConverters.*

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.pointsto.TamiFlexKey
import org.opalj.br.fpcf.properties.Contexts
import org.opalj.log.LogContext
import org.opalj.tac.common.DefinitionSitesKey

trait PointsToCallGraphKey extends CallGraphKey {

    val pointsToType: String
    val contextKey: ProjectInformationKey[? <: Contexts[?], Nothing]

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        super.requirements(project) ++ Seq(DefinitionSitesKey, VirtualFormalParametersKey) :+ contextKey
    }

    override protected[cg] def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext

        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"

        resolveAnalysisRunner(getModuleFQN("PointsTo")).toList ++ (
            if (isLibrary) resolveAnalysisRunner(getModuleFQN("Library")) else Nil
        ) ++ (
            if (TamiFlexKey.isConfigured(project)) resolveAnalysisRunner(getModuleFQN("TamiFlex")) else Nil
        )
    }

    override protected def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnalysesRegistry here
        super.registeredAnalyses(project) ++ config.getStringList(
            "org.opalj.tac.cg.PointsTo.modules"
        ).asScala.flatMap { moduleName =>
            resolveAnalysisRunner(if (moduleName.contains('.')) moduleName else getModuleFQN(moduleName))
        }
    }

    private def getModuleFQN(moduleName: String): String = {
        s"org.opalj.tac.fpcf.analyses.pointsto.${pointsToType}${moduleName}AnalysisScheduler"
    }
}
