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

    type CycleResolutionStrategy[E <: Entity, P <: Property] = (PropertyStore, EPS[E, P]) ⇒ P

    // TODO let's use a presized AtomicRefrenceArray (using SupportedPropertyKinds as the size)
    private[this] val keysLock = new ReentrantReadWriteLock

    private[this] val propertyKeyNames = ArrayBuffer.empty[String]

    private[this] val fallbackPropertyComputations = {
        ArrayBuffer.empty[(PropertyStore, Entity) ⇒ Property]
    }

    private[this] val fastTrackPropertyComputations = {
        ArrayBuffer.empty[(PropertyStore, Entity) ⇒ Option[Property]]
    }


    private[this] val cycleResolutionStrategies = {
        ArrayBuffer.empty[CycleResolutionStrategy[Entity, Property]]
    }

    private[this] var lastKeyId: Int = -1

    /**
     * Creates a new [[PropertyKey]] object that is to be shared by all properties that belong to
     * the same category.
     *
     * @param name The unique name associated with the property. To ensure
     *              uniqueness it is recommended to prepend (parts of) the package name of property.
     *              Properties defined by OPAL start with "opalj."
     *
     * @param fallbackPropertyComputation A function that returns the property that will be
     *              associated with those entities for which the property is not explicitly
     *              computed. This is generally the bottom value of the lattice.
     *
     * @param cycleResolutionStrategy The strategy that will be used to resolve unfinished cyclic
     *              computations. In the vast majority of cases it is sufficient to just commit
     *              the given value.
     * @param fastTrackPropertyComputation (Optionally) called by the property store if the property
     *              is computed in the current phase and is queried the first time
     *              (see `PropertyStore.setupPhase`). This method is expected to either provide
     *              a precise analysis very fast or to not provide a result at all.
     *              I.e., it is expected to derive only those properties that can trivially be
     *              derived precisely.
     */
    def create[E <: Entity, P <: Property](
        name:                        String,
        fallbackPropertyComputation: (PropertyStore, E) ⇒ P,
        cycleResolutionStrategy:     CycleResolutionStrategy[E, P],
        fastTrackPropertyComputation : (PropertyStore, E) ⇒ Option[P],
    ): PropertyKey[P] = {
        withWriteLock(keysLock) {
            if (propertyKeyNames.contains(name)) {
                throw new IllegalArgumentException(s"the property kind name $name is already used")
            }

            lastKeyId += 1
            if (lastKeyId == PropertyKind.SupportedPropertyKinds) {
                throw new IllegalStateException(
                    "maximum number of property keys exceeded "+
                        PropertyKind.SupportedPropertyKinds+
                        ";increase PropertyKind.SupportedPropertyKinds"
                )
            }
            propertyKeyNames += name
            fallbackPropertyComputations +=
                fallbackPropertyComputation.asInstanceOf[(PropertyStore, Entity) ⇒ Property]
            fastTrackPropertyComputations +=
                fastTrackPropertyComputation.asInstanceOf[(PropertyStore, Entity) ⇒ Option[Property]]
            cycleResolutionStrategies +=
                cycleResolutionStrategy.asInstanceOf[CycleResolutionStrategy[Entity, Property]]

            new PropertyKey(lastKeyId)
        }
    }

    def create[E <: Entity, P <: Property](
        name:                    String,
        fallbackProperty:        P,
        cycleResolutionStrategy: CycleResolutionStrategy[E, P] = (_: PropertyStore, eps: EPS[E, P]) ⇒ eps.ub,
        fastTrackPropertyComputation : (PropertyStore, E) ⇒ Option[P] = (_ : PropertyStore,_ : Entity) => None
    ): PropertyKey[P] = {
        create(
            name,
            (ps: PropertyStore, e: Entity) ⇒ fallbackProperty,
            cycleResolutionStrategy,
            fastTrackPropertyComputation
        )
    }

    //
    // Query the core properties of each property kind
    // ===============================================
    //

    /**
     * Returns the unique name of the kind of properties associated with the given key id.
     */
    def name(id: Int): String = withReadLock(keysLock) { propertyKeyNames(id) }

    final def name(pKind: PropertyKind): String = name(pKind.id)

    /**
     * @note This method is intended to be called by the framework.
     */
    def fallbackProperty[P <: Property](
        ps: PropertyStore,
        e:  Entity,
        pk: PropertyKey[P]
    ): P = {
        fallbackPropertyBasedOnPkId(ps, e, pk.id).asInstanceOf[P]
    }

    private[fpcf] def fallbackPropertyBasedOnPkId(
        ps:   PropertyStore,
        e:    Entity,
        pkId: Int
    ): Property = {
        withReadLock(keysLock) {
            val fallbackPropertyComputation = fallbackPropertyComputations(pkId)
            fallbackPropertyComputation(ps, e)
        }
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def fastTrackProperty[P <: Property](
                                           ps: PropertyStore,
                                           e:  Entity,
                                           pk: PropertyKey[P]
                                       ): Option[P] = {
        fastTrackPropertyBasedOnPkId(ps, e, pk.id).asInstanceOf[Option[P]]
    }
    private[fpcf] def fastTrackPropertyBasedOnPkId(
                                                     ps:   PropertyStore,
                                                     e:    Entity,
                                                     pkId: Int
                                                 ): Option[Property] = {
        withReadLock(keysLock) {
            val fastTrackPropertyComputation = fastTrackPropertyComputations(pkId)
            fastTrackPropertyComputation(ps, e)
        }
    }

    /**
     * @note This method is intended to be called by the framework.
     */
    def resolveCycle[E <: Entity, P <: Property](ps: PropertyStore, eps: EPS[E, P]    ): P = {
        withReadLock(keysLock) {
            cycleResolutionStrategies(eps.pk.id)(ps, eps).asInstanceOf[P]
        }
    }

    /**
     * Returns the id associated with the last created property key.
     * The id associated with the first property kind is `0`;
     * `-1` is returned if no property key is created so far.
     */
    private[fpcf] def maxId = withReadLock(keysLock) { lastKeyId }

}
