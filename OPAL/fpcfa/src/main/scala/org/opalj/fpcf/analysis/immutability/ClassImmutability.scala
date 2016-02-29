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
package fpcf
package analysis
package immutability

sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = ClassImmutability

}

/**
 * Specifies the mutability of the class instance of a type.
 *
 * @author Michael Eichberg
 */
sealed trait ClassImmutability extends Property with ClassImmutabilityPropertyMetaInformation {

    /**
     * Returns the key used by all `ClassImmutability` properties.
     */
    final def key = ClassImmutability.key

}
/**
 * Common constants use by all [[ClassImmutability]] properties associated with methods.
 */
object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {

    /**
     * The key associated with every [[ClassImmutability]] property.
     */
    final val key =
        PropertyKey.create[ClassImmutability](
            "ClassImmutability",
            // The default property that will be used if no analysis is able
            // to (directly) compute the respective property.
            MutableClassDueToUnresolvableDependency,
            // When we have a cycle all properties are necessarily at least conditionally immutable
            // hence, we can leverage the "immutability" 
            ImmutableClass

        )
}

case object UnknownClassImmutability extends ClassImmutability {
    final val isRefineable = true
}

/**
 * An instance of the respective class is effectively immutable. I.e., after creation it is not
 * possible for a client to set a field or to call a method that updates the internal state
 *
 */
case object ImmutableClass extends ClassImmutability {
    final val isRefineable = false
}

case object ConditionallyImmutableClass extends ClassImmutability {
    final val isRefineable = false
}

case object AtLeastConditionallyImmutableClass extends ClassImmutability {
    final val isRefineable = true
}

sealed trait MutableClass extends ClassImmutability {
    final val isRefineable = false
    val reason: String
}

case object MutableClassByAnalysis extends MutableClass {
    final val reason = "determined by analysis"
}

case object MutableClassDueToUnknownSupertypes extends MutableClass {
    final val reason = "the type hierarchy is upwards incomplete"
}

case object MutableClassDueToUnresolvableDependency extends MutableClass {
    final val reason = "a dependency cannot be resolved"
}

