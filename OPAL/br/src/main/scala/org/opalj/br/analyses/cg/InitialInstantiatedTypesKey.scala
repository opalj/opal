/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

/**
 *
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
object InitialInstantiatedTypesKey extends ProjectInformationKey[Iterable[ObjectType], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey."

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(ClosedPackagesKey)

    override def compute(project: SomeProject): Iterable[ObjectType] = {
        val key = ConfigKeyPrefix+"analysis"
        val configuredAnalysis = project.config.as[Option[String]](key)
        if (configuredAnalysis.isEmpty) {
            throw new IllegalArgumentException(
                "No InitialInstantiatedTypesKey configuration available; Instantiated types cannot be computed!"
            )
        }

        val fqn = configuredAnalysis.get
        val itFinder = instantiatedTypesFinder(fqn)
        itFinder.collectInstantiatedTypes(project)
    }

    private[this] def instantiatedTypesFinder(fqn: String): InstantiatedTypesFinder = {
        import scala.reflect.runtime.universe._
        val mirror = runtimeMirror(this.getClass.getClassLoader)
        val module = mirror.staticModule(fqn)
        mirror.reflectModule(module).instance.asInstanceOf[InstantiatedTypesFinder]
    }
}
