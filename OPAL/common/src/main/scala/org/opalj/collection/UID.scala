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
package collection

/**
 * Identifies objects which have a – potentially context dependent – unique id.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope. The `UIDSet` is based on comparing uids.
 *
 * @author Michael Eichberg
 */
trait UID {

    /**
     * This object's context dependent unique id.
     */
    def id: Int
}

/**
 * Identifies objects which have a – potentially context dependent – unique id.
 * I.e., this trait is implemented by objects that have – by construction -
 * unique ids in a well-defined scope.
 *
 * @note   Two objects that are ''not equal'' may have the same id, if both objects
 *         do not have the same context.
 *
 * @author Michael Eichberg
 */
trait UIDValue extends UID {

    /**
     * Two objects with a unique id are considered equal if they have the same unique id;
     * all other properties will be ignored!
     */
    final override def equals(other: Any): Boolean = {
        other match {
            case that: UID ⇒ UID.areEqual(this, that)
            case _         ⇒ false
        }
    }

    final def ===(that: UID): Boolean = UID.areEqual(this, that)

    /**
     * The unique id.
     */
    final override def hashCode: Int = id

}

/**
 * Helper methods related to data structures that have unique ids.
 */
object UID {

    final def areEqual(a: UID, b: UID): Boolean = (a eq b) || a.id == b.id

    /**
     * Returns the element stored in the given array at the position identified
     * by the [[UID]]'s unique `id` or – if no value is stored at the respective
     * position – sets the value using the value returned by `orElse` and
     * returns that value.
     */
    @inline
    final def getOrElseUpdate[T <: AnyRef](array: Array[T], uid: UID, orElse: ⇒ T): T = {
        val id = uid.id
        val t = array(id)
        if (t eq null) {
            val result = orElse
            array(id) = result
            result
        } else {
            t
        }
    }

    /**
     * Ordering for sorted collections of elements of type `UID`.
     *
     * @author Michael Eichberg
     */
    implicit object UIDBasedOrdering extends Ordering[UID] {

        def compare(a: UID, b: UID): Int = a.id - b.id

    }

}
