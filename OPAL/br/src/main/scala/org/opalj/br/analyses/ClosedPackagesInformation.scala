/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package analyses

import net.ceedubs.ficus.Ficus._

import org.opalj.log.OPALLogger.warn
import org.opalj.log.LogContext
import org.opalj.br.analyses.ClosedPackagesKey.ConfigKeyPrefix

/**
 * Determines which packages are open or closed; that is, determines to which packages code
 * - that is not yet known - can potentially be contributed to. In other words, a package is
 * considered open, if a developer is allowed to define a new type within the package's
 * namespace. If a developer cannot define a new type within the package's namespace,
 * the package is considered closed.
 *
 * An example for closed packages are the packages within the java.* namespace of the JRE.
 *
 * The set of open/closed packages may influence call-graph construction as well as other analyses.
 *
 * The concrete set of open/closed packages is specified in OPAL's configuration file. The relevant
 * configuration key is: `org.opalj.br.analyses.ClosedPackagesKey.packageContext`.
 *
 * The concrete analysis which determines the open/closed packages is instantiated reflectively
 * by [[ClosedPackagesKey]]. This facilitates the configuration of project specific closed packages.
 * The class can be configured using the config key:
 * `org.opalj.br.analyses.ClosedPackagesKey.analysis={ClosedPackagesAnalysis}``
 *
 * @note The concept of open and closed packages - and the reason why it matters - is discussed in
 *       "Call Graph Construction for Java Libraries"
 *
 * @author Michael Reif
 */
abstract class ClosedPackagesInformation extends (String ⇒ Boolean) {

    def project: SomeProject

    implicit def projectLogContext: LogContext = project.logContext

    /**
     * Returns `true` if the package with the given name is '''closed'''.
     */
    def apply(packageName: String): Boolean
}

/**
 * Treats all packages as being closed. This analysis is useable, e.g., for simple applications.
 * In this case every package is supposed to be closed since the entire code base
 * is available at analysis time.
 * Generally, not useable for libraries/frameworks/code which can be "freely" extended.
 *
 * To use this analysis set the config key:
 *  `org.opalj.br.analyses.ClosedPackagesKey.analysis`
 * to
 *  `org.opalj.br.analyses.[[ClosedCodeBase]]`
 *
 * @note An application that uses reflection to load functionality at runtime has a
 *       well-defined extension point and is not a ''simple application'' in the above sense.
 */
class ClosedCodeBase(val project: SomeProject) extends ClosedPackagesInformation {

    override def apply(packageName: String): Boolean = true
}

/**
 * A conservative [[ClosedPackagesInformation]] analysis which considers all packages
 * (except of those starting with `java`) as being open. The `java.*` packages are protected
 * by the JVM itself.
 * This analysis can safely be used by every analysis, but - depending on the context -
 * may result in imprecise analysis results.
 *
 * To use this analysis set the config key:
 *  `org.opalj.br.analyses.ClosedPackagesKey.analysis`
 * to
 *  `org.opalj.br.analyses.[[OpenCodeBase]]`
 */
class OpenCodeBase(val project: SomeProject) extends ClosedPackagesInformation {

    override def apply(packageName: String): Boolean = packageName.startsWith("java/")

}

/**
 * Determines the closed packages using a regular expression.
 *
 * The regular expression can be defined using the general configuration key:
 * `org.opalj.br.analyses.ClosedPackagesKey.closedPackages`
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.ClosedPackagesKey.closedPackages = "java&#47;*|com/sun&#47;*"
 *      }}}
 *      The previous example treats all packages starting with `java` and `com.sun`
 *      as closed.
 */
class ClosedPackagesConfiguration(val project: SomeProject) extends ClosedPackagesInformation {

    private[this] val closedPackagesRegex = {
        val closedPackages = project.config.as[Option[String]](ConfigKeyPrefix+"closedPackages")

        if (closedPackages.isEmpty)
            warn(
                "project configuration",
                "config key org.opalj.br.analyses.ClosedPackagesKey.closedPackages is not set "+
                    "- all packages are considered closed"
            )

        closedPackages.getOrElse(".*")
    }

    override def apply(packageName: String): Boolean = packageName matches closedPackagesRegex

}

/**
 * Determines the open packages using a regular expression.
 *
 * The regular expression can be defined using the general configuration key:
 * `org.opalj.br.analyses.ClosedPackagesKey.openPackages`
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses.ClosedPackagesKey.openPackages = "net/open/"
 *      }}}
 *      The previous example would consider the package `net/open/` as open.
 *
 */
class OpenPackagesConfiguration(val project: SomeProject) extends ClosedPackagesInformation {

    private[this] val openPackagesRegex = {
        val openPackages = project.config.as[Option[String]](ConfigKeyPrefix+"openPackages")

        if (openPackages.isEmpty)
            warn(
                "project configuration",
                "config key org.opalj.br.analyses.ClosedPackagesKey.openPackages not found "+
                    "- all packages are considered open."
            )

        openPackages.getOrElse(".*")
    }

    override def apply(packageName: String): Boolean = packageName matches openPackagesRegex

}
