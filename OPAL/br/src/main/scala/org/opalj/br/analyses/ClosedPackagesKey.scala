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

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get a function that determines whether a package is closed or not.
 * A package is closed if a developer cannot add new classes into the package, i.e., a developer
 * is not able to create new class in the namespace of some dependee library.
 *
 * This ''key'' reflectively instantiates the analysis that determines whether a package is closed
 * or not. All instantiated analyses have to extend the abstract [[ClosedPackagesInformation]] class.
 * To configure which analysis is used - it has to be configured in the project's configuration.
 * Please you the key '''org.opalj.br.analyses.ClosedPackagesKey.{ClosedPackagesContext}'''. Please
 * find the example configuration below:
 *
 * {{{
 *   org.opalj {
 *    br {
 *      analyses{
 *        ClosedPackagesKey {
 *          packageContext = "org.opalj.br.analyses.ClosedPackagesConfiguration"
 *          closedPackages = "java(/.*)*"
 *       }
 *     }
 *   }
 *  }
 * }}}
 *
 * @note Please see the documentation of [[ClosedPackagesInformation]] and its sub types for more
 *       information.
 * @note The default configuration is the rather conservative [[OpenCodeBase]] analysis.
 * @author Michael Reif
 */
object ClosedPackagesKey extends ProjectInformationKey[ClosedPackagesInformation] {

    private[this] final val _defautlPackageContext = "org.opalj.br.analyses.OpenCodeBase"

    final val ConfigKeyPrefix = "org.opalj.br.analyses.ClosedPackagesKey."

    /**
     * The [[ClosedPackagesKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing]] = Nil

    override protected def compute(project: SomeProject): ClosedPackagesInformation = {
        val packageContext = project.config.as[Option[String]](ConfigKeyPrefix+"packageContext").getOrElse(_defautlPackageContext)
        reify(project, packageContext).get
    }

    /*
     * Reflectively instantiates a ClosedPackagesContext. The instantiated class has to satisfy the
     * interface and needs to provide a single constructor parameterized over a Project.
     */
    private[this] def reify(project: SomeProject, packageContext: String): Option[ClosedPackagesInformation] = {
        try {
            val cls = Class.forName(packageContext)
            val cons = cls.getConstructors.head
            Some(cons.newInstance(project).asInstanceOf[ClosedPackagesInformation])
        } catch {
            case t: Throwable ⇒
                error("project configuration", s"failed to load: $packageContext", t)(GlobalLogContext)
                None
        }
    }
}