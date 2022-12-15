/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.JavaFPCFAnalysisScheduler
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator

/**
 * A [[JavaProjectInformationKey]] to compute a [[CallGraph]] based on class
 * hierarchy analysis (CHA).
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object CHACallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey, SimpleContextsKey) ++:
            super.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[JavaFPCFAnalysisScheduler] = List.empty

    override def getTypeIterator(project: SomeProject) = new CHATypeIterator(project)

}
