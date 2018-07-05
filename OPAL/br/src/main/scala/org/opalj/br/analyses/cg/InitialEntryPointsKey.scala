/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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

/**
 *
 * TODO
 * The ''key'' object to get a traversable of entry points. Entry points are particulary relevant to
 * construct call graphs.
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
 * @author Michael Reif
 */
object InitialEntryPointsKey extends ProjectInformationKey[Traversable[Method], Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.InitialEntryPointsKey."

    /**
     * The [[InitialEntryPointsKey]] depends on two other keys and queries information about closed
     * packages and must answer the question whether a method can be overridden by unknown code.
     *
     * @return `Nil`.
     */
    def requirements = Seq(ClosedPackagesKey, IsOverridableMethodKey)

    /**
     * Reflectively instantiates a ''ClosedPackagesAnalysis'' for the given project.
     * The instantiated class has to satisfy the interface and needs to provide a single
     * constructor parameterized over a Project.
     */
    override protected def compute(project: SomeProject): Traversable[Method] = {
        val key = ConfigKeyPrefix+"analysis"
        val configuredAnalysis = project.config.as[Option[String]](key)
        val entryPointFinder = configuredAnalysis
        if (entryPointFinder.isEmpty) {
            throw new IllegalArgumentException("No InitialEntryPointsKey configuration available; Entry Points cannot be computed!")
        }

        val fqn = entryPointFinder.get
        val epFinder = instantiateEntryPointFinder(fqn)
        epFinder.collectEntryPoints(project)
    }

    private[this] def instantiateEntryPointFinder(fqn: String): EntryPointFinder = {
        import scala.reflect.runtime.universe._
        val mirror = runtimeMirror(this.getClass.getClassLoader)
        val module = mirror.staticModule(fqn)
        mirror.reflectModule(module).instance.asInstanceOf[EntryPointFinder]
    }
}
