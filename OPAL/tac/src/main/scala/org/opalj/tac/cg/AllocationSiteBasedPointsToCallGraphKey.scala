/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToCallGraphKey extends PointsToCallGraphKey {

    override val pointsToType: String = "AllocationSiteBased"
    override val contextKey: ProjectInformationKey[SimpleContexts, Nothing] = SimpleContextsKey

    override def getTypeIterator(project: SomeProject) =
        new AllocationSitesPointsToTypeIterator(project)
}
