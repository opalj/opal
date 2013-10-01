/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

/**
 * Marker trait to state that each type of source element is associated
 * with its own continuous sequence of ids.
 *
 * ==Implementation Note==
 * When implemented, the following invariants have to hold:
 * - LOWEST_TYPE_ID << LOWEST_FIELD_ID << LOWEST_METHOD_ID.
 * - largest(type_id) < LOWEST_FIELD_ID
 * - largest(field_id) < LOWEST_METHOD_ID
 *
 * @author Michael Eichberg
 */
trait CategorizedSourceElementIDs extends SourceElementIDs {

    def LOWEST_TYPE_ID: Int

    def LOWEST_FIELD_ID: Int

    def LOWEST_METHOD_ID: Int
}

/**
 * Associates a source element (type, method or field declaration) with a unique id.
 *
 * Types are associated with ids >= LOWEST_TYPE_ID.
 *
 * Fields are associated with ids >= LOWEST_FIELD_ID.
 *
 * Methods are associated with ids >= LOWEST_METHOD_ID.
 *
 * Negative IDs are never assigned by this source element to ID mapping.
 * The largest id is equivalent to LOWEST_METHOD_ID + number of methods seen.
 *
 * ==Usage Scenario==
 * Ids can be used to space efficiently encode dependencies between code elements
 * E.g., assuming that the analyzed code base has less than 1.000.000 class
 * declarations, less than 4.000.000 field declarations and less than 12.777.215
 * method declarations than a single integer value can be used to encode the
 * target source element and the kind of the dependency. The highest 8 bit of
 * the integer value can be used to encode the dependency kind.
 *
 * ==Implementation Note==
 * This class is thread safe.
 *
 * @author Michael Eichberg
 * @author Thomas Schlosser
 */
trait SourceElementIDsMap extends CategorizedSourceElementIDs {

    import collection.mutable.WeakHashMap

    //
    // Associates each type with a unique ID
    //

    def LOWEST_TYPE_ID: Int = 0

    private[this] var nextTypeID = LOWEST_TYPE_ID;

    private[this] val typeIDs = WeakHashMap[Type, Int]()

    def sourceElementID(t: Type): Int = typeIDs.synchronized {
        typeIDs.getOrElseUpdate(t, { val id = nextTypeID; nextTypeID += 1; id })
    }

    //
    // Associates each field with a unique ID
    //

    def LOWEST_FIELD_ID: Int = 1000000

    private[this] var nextFieldID = LOWEST_FIELD_ID

    private[this] val fieldIDs = WeakHashMap[ObjectType, WeakHashMap[String, Int]]()

    def sourceElementID(definingObjectType: ObjectType, fieldName: String): Int =
        fieldIDs.synchronized {
            fieldIDs.
                getOrElseUpdate(definingObjectType, { WeakHashMap[String, Int]() }).
                getOrElseUpdate(fieldName, { val id = nextFieldID; nextFieldID += 1; id })
        }

    //
    // Associates each method with a unique ID
    //

    def LOWEST_METHOD_ID: Int = 5000000

    private[this] var nextMethodID = LOWEST_METHOD_ID

    private[this] val methodIDs = WeakHashMap[ReferenceType, WeakHashMap[MethodDescriptor, WeakHashMap[String, Int]]]()

    def sourceElementID(definingReferenceType: ReferenceType,
                        methodName: String,
                        methodDescriptor: MethodDescriptor): Int = methodIDs.synchronized {
        methodIDs.
            getOrElseUpdate(definingReferenceType, { WeakHashMap[MethodDescriptor, WeakHashMap[String, Int]]() }).
            getOrElseUpdate(methodDescriptor, { WeakHashMap[String, Int]() }).
            getOrElseUpdate(methodName, { val id = nextMethodID; nextMethodID += 1; id })
    }

    def allSourceElementIDs(): Iterable[Int] =
        typeIDs.synchronized {
            fieldIDs.synchronized {
                methodIDs.synchronized {
                    typeIDs.values ++
                        fieldIDs.values.flatMap(_.values) ++
                        methodIDs.values.flatMap(_.values.flatMap(_.values))
                }
            }
        }
}
