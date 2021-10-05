/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get a function that determines whether a package is closed or not.
 * See [[ClosedPackages]] for further details.
 *
 * This ''key'' reflectively instantiates the analysis that determines whether a package is closed
 * or not. The respective analysis has to extend the abstract [[ClosedPackages]] class.
 * To configure which analysis is used use the key
 * `org.opalj.br.analyses.cg.ClosedPackagesKey.analysis` to specify the name of the class which
 * implements the analysis.
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.cg {
 *          ClosedPackagesKey {
 *              analysis = "org.opalj.br.analyses.cg.ClosedPackagesConfiguration"
 *              closedPackages = "java(/.*)*"
 *          }
 *      }
 *      }}}
 *
 * @note Please see the documentation of [[ClosedPackages]] and its subtypes for more
 *       information.
 *
 * @note The default configuration is the conservative [[OpenCodeBase]] analysis.
 *
 * @author Michael Reif
 */
object ClosedPackagesKey extends ProjectInformationKey[ClosedPackages, Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.ClosedPackagesKey."

    final val DefaultClosedPackagesAnalysis = "org.opalj.br.analyses.cg.OpenCodeBase"

    /**
     * The [[ClosedPackagesKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Reflectively instantiates a ''ClosedPackagesAnalysis'' for the given project.
     * The instantiated class has to satisfy the interface and needs to provide a single
     * constructor parameterized over a Project.
     */
    override def compute(project: SomeProject): ClosedPackages = {
        try {
            val key = ConfigKeyPrefix+"analysis"
            val configuredAnalysis = project.config.as[Option[String]](key)
            val analysisClassName = configuredAnalysis.getOrElse(DefaultClosedPackagesAnalysis)
            val constructor = Class.forName(analysisClassName).getConstructors.head
            constructor.newInstance(project).asInstanceOf[ClosedPackages]
        } catch {
            case t: Throwable =>
                val m = "cannot compute closed packages; all packages are now considered open"
                error("project configuration", m, t)(project.logContext)
                new OpenCodeBase(project)
        }
    }

}
