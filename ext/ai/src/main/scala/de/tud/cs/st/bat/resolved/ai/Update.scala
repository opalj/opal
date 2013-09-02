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
package ai

/**
 * Encapsulates an updated value and qualifies the type of the update.
 *
 * In general BATAI distinguishes between updates to a value that are relevant w.r.t.
 * the abstract interpretation and those updates that just update some meta-information
 * and which do not affect the abstract interpretation and – in particular – should not
 * force BATAI to continue the abstract interpretation.
 *
 * @author Michael Eichberg
 */
sealed trait Update[+V] {

    /**
     * Merges a given `updateType` value with the type of this update and returns a
     * new `UpdateType` value.
     *
     * @see [[de.tud.cs.st.bat.resolved.ai.UpdateType]] for further details.
     */
    def &:(updateType: UpdateType): UpdateType

    /**
     * The type of this update.
     */
    def updateType: UpdateType

    /**
     * The updated value; if available.
     */
    def value: V
}

/**
 * Identifies updates where something was updated without further qualifying the update.
 *
 * ==Usage==
 * This class (and its companion object) are primarily used for pattern matching purposes.
 */
sealed abstract class SomeUpdate[V] extends Update[V]

/**
 * Facilitates matching against updates that actually encapsulate an updated value.
 */
object SomeUpdate {
    def unapply[V](update: SomeUpdate[V]): Option[V] = Some(update.value)
}

/**
 * Some part of the structure was updated such that it is required to
 * continue the abstract interpretation.
 */
final case class StructuralUpdate[V](
    value: V)
        extends SomeUpdate[V] {

    def updateType = StructuralUpdateType

    def &:(updateType: UpdateType): UpdateType = StructuralUpdateType

}
/**
 * Used to indicate that a new value is returned, but only meta-information
 * was changed that is not directly relevant to the abstract interpreter.
 *
 * ==Example==
 * If two values are merged that are seen on two different paths, but which represent the
 * same abstract value, we may want to update the meta-information about
 * the origin of the current value, but this information is not relevant
 * for the abstract interpreter itself and it will not reschedule the abstract
 * interpretation of subsequent instructions.
 */
final case class MetaInformationUpdate[V](
    value: V)
        extends SomeUpdate[V] {

    def updateType = MetaInformationUpdateType

    def &:(updateType: UpdateType): UpdateType = {
        if (updateType == StructuralUpdateType)
            updateType
        else
            MetaInformationUpdateType
    }
}

/**
 * Indicates that the (given) structure was not updated. W.r.t. the interpretation
 * BATAI does not distinguish between a `NoUpdate` and a `MetaInformationUpdate`.
 */
case object NoUpdate extends Update[Nothing] {

    def updateType = NoUpdateType

    def &:(updateType: UpdateType): UpdateType = updateType

    def value = throw new IllegalStateException("a NoUpdate contains no value")
}

/**
 * Specifies the type of an update. The type hierarchies of `Update` and `UpdateType`
 * are aligned and it is possible to conveniently switch between them. Contrary to
 * an `Update` object an `UpdateType` object never has any payload, it just characterizes
 * an update. However, by passing a value to an `UpdateType` the `UpdateType`
 * is turned into a corresponding [[de.tud.cs.st.bat.resolved.ai.Update]] object.
 *
 * ==Example==
 * {{{
 * val updateType : UpdateType = ...
 * val update : Update = updateType(<someValue>)
 * }}}
 */
sealed abstract class UpdateType {
    /**
     * Lift this update type to an `Update` of the corresponding type which contains
     * the given value.
     */
    def apply[V](value: ⇒ V): Update[V]

    /**
     * Returns `true` if `this` `UpdateType` represents the `NoUpdateType`.
     */
    def noUpdate: Boolean

    /**
     * Merges this `UpdateType` with the given one. That is, it is determined which
     * type is the more qualified one (`NoUpdateType` < `MetaInformationUpdateType` <
     * `StructuralUpdateType`) and that one is returned.
     */
    def &:(updateType: UpdateType): UpdateType

}

case object NoUpdateType extends UpdateType {
    def apply[V](value: ⇒ V): Update[V] = NoUpdate
    def &:(updateType: UpdateType): UpdateType = updateType
    def noUpdate: Boolean = true
}

case object MetaInformationUpdateType extends UpdateType {
    def apply[V](value: ⇒ V): Update[V] = MetaInformationUpdate(value)
    def noUpdate: Boolean = false
    def &:(updateType: UpdateType): UpdateType =
        if (updateType == StructuralUpdateType)
            StructuralUpdateType
        else
            this
}

case object StructuralUpdateType extends UpdateType {
    def apply[V](value: ⇒ V): Update[V] = StructuralUpdate(value)
    def noUpdate: Boolean = false
    def &:(updateType: UpdateType): UpdateType = StructuralUpdateType
}
