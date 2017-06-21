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

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.fpcf.PropertyStore

/**
 * The ''key'' object to get the project's [[org.opalj.fpcf.PropertyStore]].
 *
 * @note   It is possible to set the project's `debug` flag using the project's
 *         `org.opalj.br.analyses.SourceElementsPropertyStore.debug` config key.
 *
 * @author Michael Eichberg
 */
object PropertyStoreKey extends ProjectInformationKey[PropertyStore] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.PropertyStoreKey."

    /**
     * Used to specify the number of threads the property store should use. This
     * value is read only once when the property store is created.
     *
     * The value must be larger than 0 and should be smaller or equal to the number
     * of (hyperthreaded) cores.
     */
    @volatile var parallelismLevel: Int = Math.max(NumberOfThreadsForCPUBoundTasks, 2)

    @volatile private[this] var entityDerivationFunctions: List[SomeProject ⇒ Traversable[AnyRef]] =
        List(
            (p: SomeProject) ⇒ p.allMethods,
            (p: SomeProject) ⇒ p.allFields,
            (p: SomeProject) ⇒ p.allClassFiles
        )

    /**
     * Adds the given function to the list of entity derivation functions. An entity derivation
     * function can be used to add additional entities to the [[PropertyStore]]. An example,
     * is a function that derives – as additional entities - the set of all allocation sites.
     *
     * By default, the set of all method, all fields and all class files are added.
     *
     * '''Entity derivation functions must be registered before this key is used for
     * the first time w.r.t. a specific project.''' In general, it is recommended to add an entity
     * derivation function before or directly after the project is created.
     *
     * @param f
     */
    def addEntityDerivationFunction(f: SomeProject ⇒ Traversable[AnyRef]): Unit = {
        this.synchronized(
            entityDerivationFunctions ::= f
        )
    }

    /**
     * The [[PropertyStoreKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing]] = Nil

    /**
     * Creates a new empty property store using the current [[parallelismLevel]].
     */
    override protected def compute(project: SomeProject): PropertyStore = {
        val debug = project.config.as[Option[Boolean]](ConfigKeyPrefix+"debug").getOrElse(false)
        implicit val logContext = project.logContext

        val entities = entityDerivationFunctions.flatMap { edf ⇒ edf(project) }
        PropertyStore(
            entities,
            defaultIsInterrupted,
            parallelismLevel,
            debug,
            context = project
        )
    }
}
