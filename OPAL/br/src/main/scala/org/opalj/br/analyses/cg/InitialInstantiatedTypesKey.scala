/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import org.opalj.log.GlobalLogContext
import org.opalj.util.getObjectReflectively

import net.ceedubs.ficus.Ficus._

/**
 * Creates the [[InstantiatedTypesFinder]] as specified in the [[ConfigKeyPrefix]] config key.
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.cg {
 *          InitialInstantiatedTypesKey {
 *              analysis = "org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder"
 *          }
 *      }
 *      }}}
 *
 * @note Please see the documentation of [[InstantiatedTypesFinder]] and its subtypes for more
 *       information.
 *
 * @author Florian Kuebler
 */
object InitialInstantiatedTypesKey extends ProjectInformationKey[Iterable[ClassType], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey."

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(ClosedPackagesKey)

    override def compute(project: SomeProject): Iterable[ClassType] = {
        val key = ConfigKeyPrefix + "analysis"
        val configuredAnalysis = project.config.as[Option[String]](key)
        if (configuredAnalysis.isEmpty) {
            throw new IllegalArgumentException(
                "No InitialInstantiatedTypesKey configuration available; Instantiated types cannot be computed!"
            )
        }

        val itFinder: InstantiatedTypesFinder =
            getObjectReflectively(this, "analysis configuration", configuredAnalysis.get)(GlobalLogContext).get
        itFinder.collectInstantiatedTypes(project)
    }
}
