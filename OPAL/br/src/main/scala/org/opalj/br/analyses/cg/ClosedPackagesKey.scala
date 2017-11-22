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
package cg

import net.ceedubs.ficus.Ficus._

import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get a function that determines whether a package is closed or not.
 * See [[ClosedPackagesInformation]] for further details.
 *
 * This ''key'' reflectively instantiates the analysis that determines whether a package is closed
 * or not. The respective analysis has to extend the abstract [[ClosedPackagesInformation]] class.
 * To configure which analysis is used use the key
 * `org.opalj.br.analyses.cg.ClosedPackagesKey.analysis` to specify the name of the class which
 * implements the analysis.
 *
 * @example
 *      {{{
 *      org.opalj.br.analyses {
 *          ClosedPackagesKey {
 *              analysis = "org.opalj.br.analyses.ClosedPackagesConfiguration"
 *              closedPackages = "java(/.*)*"
 *          }
 *      }
 *      }}}
 *
 * @note Please see the documentation of [[ClosedPackagesInformation]] and its subtypes for more
 *       information.
 *
 * @note The default configuration is the conservative [[OpenCodeBase]] analysis.
 *
 * @author Michael Reif
 */
object ClosedPackagesKey extends ProjectInformationKey[String ⇒ Boolean, Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.ClosedPackagesKey."

    final val DefaultClosedPackagesAnalysis = "org.opalj.br.analyses.cg.OpenCodeBase"

    /**
     * The [[ClosedPackagesKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Reflectively instantiates a ''ClosedPackagesAnalysis'' for the given project.
     * The instantiated class has to satisfy the interface and needs to provide a single
     * constructor parameterized over a Project.
     */
    override protected def compute(project: SomeProject): String ⇒ Boolean = {
        try {
            val key = ConfigKeyPrefix+"analysis"
            val configuredAnalysis = project.config.as[Option[String]](key)
            val analysisClassName = configuredAnalysis.getOrElse(DefaultClosedPackagesAnalysis)
            val constructor = Class.forName(analysisClassName).getConstructors.head
            constructor.newInstance(project).asInstanceOf[ClosedPackagesInformation]
        } catch {
            case t: Throwable ⇒
                val m = "cannot compute closed packages; all packages are now considered open"
                error("project configuration", m, t)(project.logContext)
                (s: String) ⇒ false
        }
    }

}
