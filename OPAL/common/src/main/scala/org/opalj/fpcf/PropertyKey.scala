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
package org.opalj.fpcf

import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.collection.mutable.ArrayBuffer

import org.opalj.concurrent.Locking.withReadLock
import org.opalj.concurrent.Locking.withWriteLock

/**
 * A value object that identifies a specific kind of properties. Every entity in
 * the [[PropertyStore]] must be associated with at most one property per property kind/key.
 *
 * To create a property key use one of the companion object's [[PropertyKey$]].`create` method.
 *
 * @author Michael Eichberg
 */
final class PropertyKey[+P] private[fpcf] (val id: Int) extends AnyVal with PropertyKind {

    override def toString: String = s"PK(${PropertyKey.name(id)},id=$id)"
}

/**
 * Factory and registry for [[PropertyKey]] objects.
 *
 * @author Michael Eichberg
 */
object PropertyKey {

    type SomeEPKs = Iterable[SomeEPK]
    type CycleResolutionStrategy = (PropertyStore, SomeEPKs) ⇒ Iterable[PropertyComputationResult]

    private[this] val keysLock = new ReentrantReadWriteLock

    private[this] val propertyKeyNames = ArrayBuffer.empty[String]
    private[this] val fallbackProperties = ArrayBuffer.empty[(PropertyStore, Entity) ⇒ Property]

    // Entries in the array can be updated by an analysis!
    private[this] val cycleResolutionStrategies = ArrayBuffer.empty[CycleResolutionStrategy]

    private[this] var lastKeyId: Int = -1

    /**
     * Updates the (default) cycle resolution strategy associated with a specific kind of
     * properties.
     */
    def updateCycleResolutionStrategy[P <: Property](
        key:                     PropertyKey[P],
        cycleResolutionStrategy: CycleResolutionStrategy
    ): Unit = {
        withWriteLock(keysLock) {
            cycleResolutionStrategies(key.id) = cycleResolutionStrategy
        }
    }

    /**
     * Creates a new [[PropertyKey]] object that is to be shared by all properties that belong to
     * the same category.
     *
     * @param name The unique name associated with the property. To ensure
     *              uniqueness it is recommended to prepend (parts of) the package name of property.
     *              Properties defined by OPAL start with "opalj."
     *
     * @param fallbackProperty A function that returns the property that will be associated
     *              with those entities for which the property is not explicitly computed.
     *
     * @param cycleResolutionStrategy The strategy that will be used to resolve unfinished cyclic
     *              computations. The strategy can be adapted by an analysis to its own needs.
     *              In general, the cycle resolution strategy can query the (potential) properties
     *              of the cycle's entities, but not the properties of other entities as this
     *              could trigger direct/lazy computations which is not supported.
     */
    def create[P <: Property](
        name:                    String,
        fallbackProperty:        (PropertyStore, Entity) ⇒ P,
        cycleResolutionStrategy: CycleResolutionStrategy
    ): PropertyKey[P] = {
        withWriteLock(keysLock) {
            if (propertyKeyNames.contains(name)) {
                throw new IllegalArgumentException(s"the property kind name $name is already used")
            }

            lastKeyId += 1
            propertyKeyNames += name
            fallbackProperties += fallbackProperty
            cycleResolutionStrategies += cycleResolutionStrategy
            new PropertyKey(lastKeyId)
        }
    }

    def create[P <: Property](
        name:                    String,
        fallback:                P,
        cycleResolutionStrategy: CycleResolutionStrategy
    ): PropertyKey[P] = {
        create(name, (ps: PropertyStore, e: Entity) ⇒ fallback, cycleResolutionStrategy)
    }

    def create[P <: Property](
        name:                    String,
        fallback:                (PropertyStore, Entity) ⇒ P,
        cycleResolutionProperty: P
    ): PropertyKey[P] = {
        create(
            name,
            fallback,
            (ps: PropertyStore, epks: SomeEPKs) ⇒ Seq(Result(epks.head.e, cycleResolutionProperty))
        )
    }

    def create[P <: Property](
        name:                    String,
        fallback:                P,
        cycleResolutionProperty: P
    ): PropertyKey[P] = {
        create(name, (ps: PropertyStore, e: Entity) ⇒ fallback, cycleResolutionProperty)
    }

    def create[P <: Property](name: String, fallback: P): PropertyKey[P] = {
        create(name, fallback, fallback)
    }

    //
    // Query the core properties of each property kind
    // ===============================================
    //

    /**
     * Returns the unique name of the kind of properties associated with the given key id.
     */
    def name(id: Int): String = withReadLock(keysLock) { propertyKeyNames(id) }

    /**
     * @note This method is intended to be called by the framework.
     */
    def fallbackProperty[P <: Property](
        ps: PropertyStore,
        e:  Entity,
        pk: PropertyKey[P]
    ): P = {
        withReadLock(keysLock) {
            fallbackProperties(pk.id)(ps, e).asInstanceOf[P]
        }
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def resolveCycle(
        ps:   PropertyStore,
        epks: Iterable[SomeEPK]
    ): Iterable[PropertyComputationResult] = {
        withReadLock(keysLock) {
            val epk = epks.head
            cycleResolutionStrategies(epk.pk.id)(ps, epks)
        }
    }

    /**
     * Returns the id associated with the last created property key.
     * The id associated with the first property kind is `0`;
     * `-1` is returned if no property key is created so far.
     */
    private[fpcf] def maxId = withReadLock(keysLock) { lastKeyId }

}
