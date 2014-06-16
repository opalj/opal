/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai

import scala.language.higherKinds

/**
 * Encapsulates an updated value and qualifies the type of the update.
 *
 * In general OPAL distinguishes between updates to a value that are relevant w.r.t.
 * the abstract interpretation and those updates that just update some meta-information
 * and which do not affect the abstract interpretation and – in particular – do not
 * force the framework to continue the abstract interpretation.
 *
 * @author Michael Eichberg
 */
sealed trait Update[+V] {

    type ThisType[V] <: Update[V]

    /**
     * Merges a given `updateType` value with the type of this update and returns a
     * new `UpdateType` value.
     *
     * @see [[org.opalj.ai.UpdateType]] for further details.
     */
    def &:(updateType: UpdateType): UpdateType

    def isStructuralUpdate: Boolean = false

    def isNoUpdate: Boolean = false

    def isMetaInformationUpdate: Boolean = false

    /**
     * The ''type'' of this update.
     */
    def updateType: UpdateType

    /**
     * The updated value; if available.
     */
    @throws[java.lang.IllegalStateException]("if the value was not updated")
    def value: V

    /**
     * Creates a new `Update` object of the same type with the given value.
     */
    @throws[java.lang.IllegalStateException]("if the type of this update is NoUpdate")
    def updateValue[NewV](newValue: NewV): ThisType[NewV]
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
 * Characterizes updates where the abstract state was updated such that it is required to
 * continue the abstract interpretation.
 */
final case class StructuralUpdate[V](
    value: V)
        extends SomeUpdate[V] {

    type ThisType[V] = StructuralUpdate[V]

    override def isStructuralUpdate: Boolean = true

    override def updateType = StructuralUpdateType

    override def &:(updateType: UpdateType): UpdateType = StructuralUpdateType

    override def updateValue[NewV](newValue: NewV): StructuralUpdate[NewV] =
        StructuralUpdate(newValue)
}
/**
 * Characterizes an update that did not affect the abstract state but instead just
 * updated some meta information.
 *
 * In general, the abstract interpretation framework handles `NoUpdate`s and
 * `MetaInformationUpdate`s in the same way.
 *
 * ==Example==
 * If two values are merged that are seen on two different paths, but which represent the
 * same abstract value, we may want to update the meta-information about
 * the origin of the current value, but this information may not be part of the abstract
 * state and hence, is not relevant for the abstract interpreter. In this case the
 * interpreter will not reschedule subsequent instructions. ''However, whether or not
 * the information about the origin of a value is considered to be part of the abstract
 * state is a decision of the domain.''
 */
final case class MetaInformationUpdate[V](
    value: V)
        extends SomeUpdate[V] {

    type ThisType[V] = MetaInformationUpdate[V]

    override def isMetaInformationUpdate: Boolean = true

    override def updateType = MetaInformationUpdateType

    override def &:(updateType: UpdateType): UpdateType = {
        if (updateType == StructuralUpdateType)
            updateType
        else
            MetaInformationUpdateType
    }

    override def updateValue[NewV](newValue: NewV): MetaInformationUpdate[NewV] =
        MetaInformationUpdate(newValue)
}

/**
 * Indicates that the (given) structure was not updated.
 *
 * @note The abstract interpretation framework itself does not distinguish between a
 *      `NoUpdate` and a `MetaInformationUpdate`; the abstract interpretation will not
 *      be continued in both cases.
 */
case object NoUpdate extends Update[Nothing] {

    type ThisType[V] = NoUpdate.type

    override def isNoUpdate: Boolean = true

    override def updateType = NoUpdateType

    override def &:(updateType: UpdateType): UpdateType = updateType

    override def value =
        throw new IllegalStateException("a NoUpdate contains no value")

    override def updateValue[NewV](newValue: NewV): NoUpdate.type =
        throw new IllegalStateException("updating the value of a NoUpdate is not supported")
}
