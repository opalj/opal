/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

// TODO document
/**
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses {
 *          InitialEntryPointKey {
 *              analysis = "org.opalj.br.analyses.ApplicationEntryPointFinder"
 *          }
 *      }
 *      }}}
 *
 * @note Please see the documentation of [[EntryPointFinder]] and its subtypes for more
 *       information.
 *
 * @author Florian Kuebler
 */
object InitialInstantiatedTypesKey extends ProjectInformationKey[Traversable[ObjectType], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey."

    def requirements = Seq(
        TypeExtensibilityKey, ClosedPackagesKey, IsOverridableMethodKey, InitialEntryPointsKey
    )

    override protected def compute(project: SomeProject): Traversable[ObjectType] = {
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
