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
package fpcf

/*
import java.util.concurrent.{ConcurrentHashMap ⇒ JCHMap}

/**
 *
 * @param data The core array which contains - for each property key - the map of entities to the
 *             derived property. The map is lazily initialized.
 */
class PropertyStore2 private (
        val entities:        Set[Entity],
        private val data:    Array[JCHMap[Entity, EntityCell]] = new Array(1024 /* TODO MAKE IT CONFIGURABLE .. A NUMBER MUCH LARGER THAN THE LARGEST PROPERTY_KEY*/ ),
        @volatile var debug: Boolean
) {

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     * @note The returned collection can be used to create an [[IntermediateResult]].
     */
    def apply[P <: Property](e: Entity, pk: PropertyKey[P]): EOptionP[e.type, P] = {
        data(pk).get(e) match {
            case null                 ⇒ EPK(e, pk)
            case ec: EntityCell[_, _] ⇒ EP(e, ec.p)
        }
    }

    def set(e: Entity, p: Property): Unit = {

    }

    def run(f: Entity ⇒ PropertyComputationResult): Unit = {

    }

}

private[FPCF] case class EntityCell[+E <: Entity, +P <: Property]() {

    def p: P = ???
}
*/
