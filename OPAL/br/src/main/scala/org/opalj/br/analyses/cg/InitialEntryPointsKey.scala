/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import org.opalj.util.getObjectReflectively

import net.ceedubs.ficus.Ficus._

/**
 * The ''key'' object to get a traversable of entry points. Entry points are particularly relevant
 * to construct call graphs.
 * See [[InitialEntryPointsKey]] for further details.
 *
 * This ''key'' reflectively instantiates the analysis that determines the program's entry points.
 * The respective analysis has to extend the trait [[InitialEntryPointsKey]] class.
 *
 * To configure which analysis is used use the key
 * `org.opalj.br.analyses.cg.InitialEntryPointKey.analysis` to specify the name of the class which
 * implements the analysis.
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.cg {
 *          InitialEntryPointKey {
 *              analysis = "org.opalj.br.analyses.cg.ApplicationEntryPointFinder"
 *          }
 *      }
 *      }}}
 *
 * @note Please see the documentation of [[EntryPointFinder]] and its subtypes for more
 *       information.
 *
 * @author Michael Reif
 */
object InitialEntryPointsKey extends ProjectInformationKey[Iterable[Method], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialEntryPointsKey."
    final val ConfigKey = ConfigKeyPrefix + "analysis"

    /**
     * The [[InitialEntryPointsKey]] depends on three other keys and queries information about closed
     * packages, must answer the question whether a method can be overridden by unknown code, and
     * performs checks whether types are extensible or not. Additionally, required keys from the configured
     * EntryPointFinder are added.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): ProjectInformationKeys = {
        val entryPointFinderRequiredKeys = getEntryPointFinder(project).requirements(project)
        Seq(TypeExtensibilityKey, ClosedPackagesKey, IsOverridableMethodKey) ++ entryPointFinderRequiredKeys
    }

    override def compute(project: SomeProject): Iterable[Method] = {
        val epFinder: EntryPointFinder = getEntryPointFinder(project)
        epFinder.collectEntryPoints(project)
    }

    private[this] def getEntryPointFinder(project: SomeProject): EntryPointFinder = {
        val configuredAnalysis = project.config.as[Option[String]](ConfigKey)
        val entryPointFinder = configuredAnalysis
        if (entryPointFinder.isEmpty) {
            throw new IllegalArgumentException(
                s"entry points cannot be computed due to missing configuration of $ConfigKey"
            )
        }

        getObjectReflectively(entryPointFinder.get, this, "analysis configuration").get
    }
}
