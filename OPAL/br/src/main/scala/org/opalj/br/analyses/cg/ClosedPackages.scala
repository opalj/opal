/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.warn

/**
 * Determines which packages are open or closed; that is, determines to which packages code
 * - which is not yet known - can potentially be contributed to. In other words, a package is
 * considered open, if a developer is allowed to define a new type within the package's
 * namespace. If a developer cannot define a new type within the package's namespace,
 * the package is considered closed.
 *
 * An example for closed packages are the packages within the java.* namespace of the JRE.
 * Additionally, with Java 9 packages defined in modules can be considered closed.
 *
 * The set of open/closed packages may influence call-graph construction as well as other analyses.
 *
 * The concrete set of open/closed packages is specified in OPAL's configuration file. The relevant
 * configuration key is: `org.opalj.br.analyses.cg.ClosedPackagesKey`.
 *
 * The concrete analysis which determines the open/closed packages is instantiated reflectively
 * by [[ClosedPackagesKey]]. This facilitates the configuration of project specific closed packages.
 * The class can be configured using the config key:
 * `org.opalj.br.analyses.cg.ClosedPackagesKey.analysis={ClosedPackagesAnalysis}`
 *
 * @note The concept of open and closed packages - and the reason why it matters - is discussed in
 *       "Call Graph Construction for Java Libraries"
 *
 * @author Michael Reif
 */
abstract class ClosedPackages extends (String => Boolean) {

    def project: SomeProject

    implicit def projectLogContext: LogContext = project.logContext

    /**
     * Returns `true` if the package with the given name is '''closed'''.
     */
    final override def apply(packageName: String): Boolean = isClosed(packageName)

    def isClosed(packageName: String): Boolean

}

/**
 * Treats all packages as being closed. This analysis is useable, e.g., for simple applications
 * which have no concept of plug-ins or a similar mechanism.
 * In this case every package is supposed to be closed since the entire code base
 * is available at analysis time.
 * Generally, not useable for libraries/frameworks/code which can be "freely" extended.
 *
 * To use this analysis set the config key:
 *  `org.opalj.br.analyses.cg.ClosedPackagesKey.analysis`
 * to
 *  `org.opalj.br.analyses.cg.[[AllPackagesClosed]]`
 *
 * @note An application that uses reflection to load functionality at runtime has a
 *       well-defined extension point and is not a ''simple application'' in the above sense.
 */
class AllPackagesClosed(val project: SomeProject) extends ClosedPackages {

    /** Always returns true. */
    override def isClosed(packageName: String): Boolean = true

}

/**
 * A conservative [[ClosedPackages]] analysis which considers all packages
 * (except of those starting with `java`) as being open. The `java.*` packages are protected
 * by the JVM itself.
 * This analysis can safely be used by every analysis, but - depending on the context -
 * may result in imprecise analysis results.
 *
 * To use this analysis set the config key:
 *  `org.opalj.br.analyses.cg.ClosedPackagesKey.analysis`
 * to
 *  `org.opalj.br.analyses.cg.[[OpenCodeBase]]`
 */
class OpenCodeBase(val project: SomeProject) extends ClosedPackages {

    override def isClosed(packageName: String): Boolean = packageName.startsWith("java/")

}

/**
 * Determines the closed packages using a regular expression.
 *
 * The regular expression can be defined using the general configuration key:
 * `org.opalj.br.analyses.cg.ClosedPackagesKey.closedPackages`
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.cg.ClosedPackagesKey.closedPackages = "java&#47;*|com/sun&#47;*"
 *      }}}
 *      The previous example treats all packages starting with `java` and `com.sun`
 *      as closed.
 */
class ClosedPackagesConfiguration(val project: SomeProject) extends ClosedPackages {

    private[this] val closedPackagesRegex = {
        val configKey = ClosedPackagesKey.ConfigKeyPrefix+"closedPackages"
        val closedPackages = project.config.as[Option[String]](configKey)

        if (closedPackages.isEmpty) {
            warn(
                "project configuration",
                s"$configKey is not set - all packages are considered closed"
            )
        }

        closedPackages.getOrElse(".*")
    }

    override def isClosed(packageName: String): Boolean = packageName matches closedPackagesRegex

}
