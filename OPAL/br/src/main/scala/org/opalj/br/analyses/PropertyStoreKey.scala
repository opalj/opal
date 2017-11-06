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

import java.util.concurrent.ConcurrentLinkedQueue

import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf

import scala.collection.JavaConverters._

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext

/**
 * The ''key'' object to get the project's [[org.opalj.fpcf.PropertyStore]].
 *
 * @note   It is possible to set the project's `debug` flag using the project's
 *         `org.opalj.br.analyses.PropertyStore.debug` config key.
 *
 * @author Michael Eichberg
 */
object PropertyStoreKey
    extends ProjectInformationKey[PropertyStore, ConcurrentLinkedQueue[EntityDerivationFunction]] {

    /**
     * Used to specify the number of threads the property store should use. This
     * value is read only once when the property store is created.
     *
     * The value must be larger than 0 and should be smaller or equal to the number
     * of (hyperthreaded) cores.
     */
    @volatile var parallelismLevel: Int = Math.max(NumberOfThreadsForCPUBoundTasks, 2)

    def defaultEntityDerivationFunctions(
        p: SomeProject
    ): ConcurrentLinkedQueue[EntityDerivationFunction] = {
        val edfs = new ConcurrentLinkedQueue[EntityDerivationFunction]()
        edfs.add(() ⇒ (p.allMethods, typeOf[Iterable[Method]], p.allMethods))
        edfs.add(() ⇒ (p.allFields, typeOf[Iterable[Field]], p.allFields))
        edfs.add(() ⇒ (p.allClassFiles, typeOf[Iterable[ClassFile]], p.allClassFiles))
        edfs
    }

    /**
     * Adds the given function to the list of entity derivation functions for the given project.
     * The set of entities is computed on demand when this key's value is requested.
     *
     * An example, is a function that derives – as additional entities - the set of all
     * allocation sites.
     *
     * By default, the set of all method, all fields and all class files are added.
     *
     * '''Entity derivation functions must be registered before this key is used for
     * the first time w.r.t. a specific project.''' In general, it is recommended to add an entity
     * derivation function before or directly after the project is created. Furthermore, it
     * is generally recommended to use a(nother) key to add the entities and to use that key's
     * entities to drive the entity derivation function. (See [[makeAllocationSitesAvailable]] for
     * further details.)
     *
     * @param f A function which computes
     *          (1) the set of entities and
     *          (2) (optionally) a data structure – which typically makes the set of computed
     *          properties available. This data structure is added as a context value to the
     *          property store; the key is the specified generic type. E.g., `Iterable[Method]`
     *          for the set of methods, `Iterable[Field]` for the set of fields and
     *          `Iterable[ClassFile]` for the default set of class files.
     *
     */
    def addEntityDerivationFunction[T <: AnyRef: TypeTag](
        project: SomeProject
    )(
        f: ⇒ (Traversable[AnyRef], T)
    ): Unit = this.synchronized {
        project.getOrCreateProjectInformationKeyInitializationData(
            this, defaultEntityDerivationFunctions(project)
        ).add {
            () ⇒ { val (es, ctxValue) = f; (es, typeOf[T], ctxValue: AnyRef) }
        }

    }

    /**
     * Makes the set of [[AllocationSites]] available to the property store that is created
     * for the respective project later on. I.e., this method must be called, before this key
     * is used to get the project's property store.
     */
    def makeAllocationSitesAvailable(p: SomeProject): Unit = {
        addEntityDerivationFunction(p) { AllocationSitesKey.entityDerivationFunction(p) }
    }

    /**
     * Makes the set of [[FormalParameters]] available to the property store that is created
     * for the respective project later on. I.e., this method must be called, before this key
     * is used to get the project's property store.
     */
    def makeFormalParametersAvailable(p: SomeProject): Unit = {
        addEntityDerivationFunction(p) { FormalParametersKey.entityDerivationFunction(p) }
    }

    /**
     * Makes the set of [[VirtualFormalParameters]] available to the property store that is created
     * for the respective project later on. I.e., this method must be called, before this key
     * is used to get the project's property store.
     */
    def makeVirtualFormalParametersAvailable(p: SomeProject): Unit = {
        addEntityDerivationFunction(p) { VirtualFormalParametersKey.entityDerivationFunction(p) }
    }

    /**
     * Makes the set of [[org.opalj.br.VirtualForwardingMethod]] available to the property store
     * that is created for the respective project later on. I.e., this method must be called,
     * before this key is used to get the project's property store.
     */
    def makeVirtualForwardingMethodsAvailable(p: SomeProject): Unit = {
        addEntityDerivationFunction(p) { VirtualForwardingMethodsKey.entityDerivationFunction(p) }
    }

    /**
     * The [[PropertyStoreKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Creates a new empty property store using the current [[parallelismLevel]].
     */
    override protected def compute(project: SomeProject): PropertyStore = {
        implicit val logContext = project.logContext

        val entityDerivationFunctions = project.
            getProjectInformationKeyInitializationData(this).
            getOrElse(defaultEntityDerivationFunctions(project))

        var context: List[PropertyStoreContext[AnyRef]] = List(
            PropertyStoreContext[org.opalj.br.analyses.SomeProject](project)
        )
        val entities = entityDerivationFunctions.asScala.flatMap { edf ⇒
            val (entities, ctxKey, ctxValue) = edf()
            if (ctxKey != typeOf[Nothing]) {
                context ::= PropertyStoreContext[AnyRef](ctxKey, ctxValue)
            }
            entities
        }
        PropertyStore(entities, defaultIsInterrupted, parallelismLevel, context: _*)
    }
}
