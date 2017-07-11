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
package properties

/**
 * Describes lifetime of allocated objects. If the lifetime of an object 'o' that is
 * allocated in a method 'm' and in thread 't' exceeds the lifetime of 'm', we say 'o' has the
 * property MethodEscape. Whereas if the lifetime of 'o' exceeds the lifetime of 't', 'o' has
 * the property GlobalEscape. Otherwise it has the property NoEscape.
 * The property values are ordered as follows: NoEscape < MethodEscape < GlobalEscape.
 * Algorithms can improve their efficiency by over approximating this property, i.e. for object
 * 'o' with actual property 'p' it is okay to say 'o' has property 'p*' if 'p'<'p*'.
 * @author Florian Kuebler
 */
sealed trait EscapePropertyMetaInforation extends PropertyMetaInformation {
    final type Self = EscapeProperty
}

sealed abstract class EscapeProperty extends Property with EscapePropertyMetaInforation {
    final def key = EscapeProperty.key
}

/**
 * Refers to the EscapeProperties mentioned by Kotzmann and Moessenboeck: Escape Analysis in the
 * Context of Dynamic Compilation and Deoptimization (2005)
 */
object EscapeProperty extends EscapePropertyMetaInforation {
    final val key: PropertyKey[EscapeProperty] = PropertyKey.create(
        // Name of the property
        "EscapeProperty",
        // Fallback value
        GlobalEscape,
        // cycle-resolution strategy
        GlobalEscape
    )
}

/**
 * An object that does not leaf the scope of the method it was allocated in.
 */
case object NoEscape extends EscapeProperty {
    final val isRefineable = false
}

/**
 *
 */
case object ConditionallyNoEscape extends EscapeProperty {
    final val isRefineable: Boolean = true
}

/**
 * An object that is passed into a method but is not accessible from another thread.
 */
case object MethodEscape extends EscapeProperty {
    final val isRefineable = false
}

/**
 * An object that is accessible from another thread.
 */
case object GlobalEscape extends EscapeProperty {
    final val isRefineable = false
}

