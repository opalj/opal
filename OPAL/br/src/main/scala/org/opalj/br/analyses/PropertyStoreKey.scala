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

import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.typeOf

import net.ceedubs.ficus.Ficus._
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext

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

    // NOTE: the returned type specifies the type of the ctx value (the last AnyRef)
    @volatile private[this] var entityDerivationFunctions: List[SomeProject ⇒ (Traversable[AnyRef], Type, AnyRef)] =
        List(
            (p: SomeProject) ⇒ (p.allMethods, typeOf[Iterable[Method]], p.allMethods),
            (p: SomeProject) ⇒ (p.allFields, typeOf[Iterable[Field]], p.allFields),
            (p: SomeProject) ⇒ (p.allClassFiles, typeOf[Iterable[ClassFile]], p.allClassFiles)
        )

    /**
     * Adds the given function to the list of entity derivation functions which – given a project –
     * compute a set of entities, which should be managed by the store; i.e., an entity derivation
     * function can be used to add additional entities to the [[org.opalj.fpcf.PropertyStore]].
     * An example, is a function that derives – as additional entities - the set of all #
     * allocation sites.
     *
     * By default, the set of all method, all fields and all class files are added.
     *
     * '''Entity derivation functions must be registered before this key is used for
     * the first time w.r.t. a specific project.''' In general, it is recommended to add an entity
     * derivation function before or directly after the project is created.
     *
     * @param f A function that takes a project and which computes (1) the set of of entities and
     *          (2) (optionally) a data structure – which typically makes the set of computed
     *          properties available – which is added as a context value to the property store; the
     *          key is the specified generic type. E.g., `Iterable[Method]` for the set of
     *          methods, `Iterable[Field]` for the set of fields and `Iterable[ClassFile]` for the
     *          default set of class files.
     *
     */
    def addEntityDerivationFunction[T <: AnyRef: TypeTag](
        f: SomeProject ⇒ (Traversable[AnyRef], T)
    ): Unit = {
        this.synchronized(
            entityDerivationFunctions ::= (
                (p: SomeProject) ⇒ {
                    val (es, ctxValue) = f(p)
                    (es, typeOf[T], ctxValue)
                }
            )
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

        var context: List[PropertyStoreContext[AnyRef]] = Nil
        val entities = entityDerivationFunctions.flatMap { edf ⇒
            val (entities, ctxKey, ctxValue) = edf(project)
            if (ctxKey != typeOf[Nothing]) {
                context ::= PropertyStoreContext[AnyRef](ctxKey, ctxValue)
            }
            entities
        }
        PropertyStore(
            entities,
            defaultIsInterrupted,
            parallelismLevel,
            debug,
            context: _*
        )
    }
}
