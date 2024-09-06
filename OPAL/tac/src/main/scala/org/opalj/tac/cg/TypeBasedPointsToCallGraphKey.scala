/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.SimpleContextProvider
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeIterator

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object TypeBasedPointsToCallGraphKey extends PointsToCallGraphKey {

    override val pointsToType: String = "TypeBased"
    override val contextKey: ProjectInformationKey[SimpleContexts, Nothing] = SimpleContextsKey

    override def getTypeIterator(project: SomeProject): TypeIterator =
        new TypeIterator(project) with TypesBasedPointsToTypeIterator with SimpleContextProvider

}
