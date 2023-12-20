/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator

/**
 * Pseudo CallGraphKey that can be used to declare methods reachable without computing a call graph.
 * This will NOT provide any Callee properties!
 *
 * Methods deemed reachable are set via this key's initialization data. If not set, all methods of the project are used.
 *
 * Types are resolved using the CHATypeIterator, i.e., on the fly with CHA precision.
 */
object NoCallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(PropertyStoreKey, FPCFAnalysesManagerKey, SimpleContextsKey)
    }

    override protected def callGraphSchedulers(project: SomeProject): Iterable[FPCFAnalysisScheduler] = List.empty

    override def getTypeIterator(project: SomeProject) = new CHATypeIterator(project)

    override protected def runAnalyses(project: SomeProject, ps: PropertyStore): Unit = {
        val methods = project.getProjectInformationKeyInitializationData(this).getOrElse(project.allMethods)

        methods.foreach { method =>
            ps.set(method, OnlyCallersWithUnknownContext)
        }
    }
}
