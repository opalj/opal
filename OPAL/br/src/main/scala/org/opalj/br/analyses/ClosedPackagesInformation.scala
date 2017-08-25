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
import org.opalj.br.analyses.ClosedPackagesKey.ConfigKeyPrefix
import org.opalj.log.OPALLogger

/**
 * A common trait for all analyses that compute which packages are open or closed. A package is
 * considered open, if a developer is allowed to define a new class/interface within the package's
 * namespace. If a developer cannot define a new class/interface within the package's namespace,
 * the package is considered closed. An example for closed packages are the packages within the
 * java.* namespace of the JRE.
 *
 * The outcome of this analysis influences the call-graph construction as well as other analyses.
 * Please make sure to configure the correct analysis within the configuration file. The relevant
 * configuration key is: "org.opalj.br.analyses.ClosedPackagesKey.packageContext".
 *
 * Sub classes are reflectively instantiated within [[ClosedPackagesKey]] to provide a possibility
 * to configure an analysis-specific understanding of closed packages. Those classes can be configured
 * under the config key: "org.opalj.br.analyses.ClosedPackagesKey.{ClosedPackagesAnalysis}"
 *
 * @note The concept of open and closed packages - and the reason it matters - is discussed in
 *       "Call Graph Construction for Java Libraries"
 *
 * @author Michael Reif
 */
abstract class ClosedPackagesInformation(val project: SomeProject) extends (String ⇒ Boolean) {

    def apply(packageName: String): Boolean
}

/**
 * A simple [[ClosedPackagesInformation]] that can be used for rather simple applications without any
 * dynamic extension points. In the context of a simple applications every package is supposed to
 * be closed since the entire code base is available at analysis time.
 *
 * Config key: "org.opalj.br.analyses.ClosedPackagesKey.[[ClosedCodeBase]]"
 *
 * @note An application may allow reflective instantiation of unknown code, and therefore, has a
 *       well-defined extension point. Such an application is not considered "simple".
 */
class ClosedCodeBase(project: SomeProject) extends ClosedPackagesInformation(project) {

    override def apply(packageName: String): Boolean = true
}

/**
 * A conservative [[ClosedPackagesInformation]] that can safely be used in every analysis but may result
 * in imprecise analysis results.
 *
 * All packages are considered open, except those, where the access is strictly prohibited by the
 * JVM itself. I.e., the JVM strictly prohibits the access of all "java.*" packages.
 *
 * Config key: "org.opalj.br.analyses.ClosedPackagesKey.[[OpenCodeBase]]"
 */
class OpenCodeBase(project: SomeProject) extends ClosedPackagesInformation(project) {

    override def apply(packageName: String): Boolean = {
        packageName.startsWith("java.")
    }
}

/**
 * A [[org.opalj.br.analyses.ClosedPackagesInformation]] that allows to define the number of closed packages over a regular
 * expression.
 *
 * The regular expression can be defined over the general configuration with the key:
 * {{{
 *   config key: "org.opalj.br.analyses.ClosedPackagesKey.closedPackages"
 *
 *   example:
 *
 *   org.opalj.br.analyses.ClosedPackagesKey.closedPackages = "java/*|com/sun/*"
 *
 *   The previous example would consider all packages (incl. subpackages) under "java" and "com.sun"
 *   as closed.
 * }}}
 * */*/
 */
class ClosedPackagesConfiguration(project: SomeProject) extends ClosedPackagesInformation(project) {

    private[this] lazy val closedPackagesRegex = {
        val closedPackages = project.config.as[Option[String]](
            ConfigKeyPrefix+"closedPackages"
        )

        if (closedPackages.isEmpty)
            OPALLogger.warn(
                "project configuration",
                "'closedPackages' config key not found - all packages are considered closed."
            )(project.logContext)

        closedPackages.getOrElse(".*")
    }

    override def apply(packageName: String): Boolean = {
        packageName matches closedPackagesRegex
    }
}

/**
 * A [[org.opalj.br.analyses.ClosedPackagesInformation]] that allows to define the number of open
 * packages over a regular expression.
 *
 * The regular expression can be defined over the general configuration with the key:
 * {{{
 *   config key: "org.opalj.br.analyses.ClosedPackagesKey.openPackages"
 *
 *   example:
 *
 *   org.opalj.br.analyses.ClosedPackagesKey.openPackages = "net/open/"
 *
 *   The previous example would consider the "net/open/" as open.
 * }}}
 */
class OpenPackagesConfiguration(project: SomeProject) extends ClosedPackagesInformation(project) {

    private[this] lazy val openPackagesRegex = {
        val openPackages = project.config.as[Option[String]](
            ConfigKeyPrefix+"openPackages"
        )

        if (openPackages.isEmpty)
            OPALLogger.warn(
                "project configuration",
                "'openPackages' config key not found - all packages are considered open."
            )(project.logContext)

        openPackages.getOrElse(".*")
    }

    override def apply(packageName: String): Boolean = {
        packageName matches openPackagesRegex
    }
}