/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.fpcf.scheduling.FPCFAnalysisScheduler
import org.opalj.si.ProjectInformationKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on class
 * hierarchy analysis (CHA).
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object CHACallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey, SimpleContextsKey) ++:
            super.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = List.empty

    override def getTypeIterator(project: SomeProject) = new CHATypeIterator(project)

}
