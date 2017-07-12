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

sealed trait EscapePropertyMetaInforation extends PropertyMetaInformation {
    final type Self = EscapeProperty
}

sealed abstract class EscapeProperty extends Property with EscapePropertyMetaInforation {
    final def key = EscapeProperty.key
}

/**
 * Describes lifetime of object instance. This is classically used for compiler optimizations
 * such as scalar replacement, stack allocation or removal of synchronization.
 * Choi et al. [1] describes two predicates that can be used to describe the properties relevant
 * to escape information.
 *
 *      "Let O be an object instance and M be a method invocation. O is said to escape M, denoted as
 *      Escapes(O, M), if the lifetime of O may exceed the lifetime of M."
 *
 *      "Let O be an object instance and T be a thread (instance). O is said to escape T, again
 *      denoted as Escapes(O, T), if another thread, T’ != T, may access O."
 *
 * Furthermore it holds that "For any object O, !Escapes(O, M) implies !Escapes(O, T), where method
 * M is invoked in thread T."
 *
 * [[NoEscape]] now refers to the property of an object instance O created in method M for that
 * !Escapes(O, M) holds. This implies that there is no method M' != M that can access O (at least
 * when disregarding reflection and native code). Objects with this property can be allocated at
 * the stack or even scalar-replaced [2].
 *
 * An object instance O created in method M and thread T has the property [[ArgEscape]], if it holds
 * Escapes(O, M) but not Escapes(O, T). This is usually the case if O is passed as parameter to
 * a method which does not let O escape globally. For objects that have the property [[ArgEscape]]
 * no synchronization is needed. Note that Kotzmann and Moessenboeck [2] denotes the exact same
 * property as MethodEscape. Furthermore they describe these objects also as stack-allocatable.
 *
 * An object instance O created in method M and thread T has the property [[GlobalEscape]], if it
 * holds that Escapes(O, M) and Escapes(O, T). For example this is the case if O gets assigned to
 * a static field but also if it is returned by M or assigned to a field of an object that has
 * also [[GlobalEscape]] as property.
 * There is room for a discussion to introduce a stage between [[ArgEscape]] and
 * [[GlobalEscape]] to describe the fact that O is returned by M but not accessible by another
 * thread. For now we stick with the literature.
 * Objects that have the property [[GlobalEscape]] have to be allocated on the heap and
 * synchronization mechanisms can not be removed.
 *
 * The property values are (totally) ordered as follows: [[NoEscape]] < [[ArgEscape]] <
 * [[GlobalEscape]].
 * Algorithms can improve their efficiency by over approximating this property, i.e. for object
 * instance O with actual property P it is okay to say O has property P' if P < P'.
 *
 *
 * @author Florian Kuebler
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
 * "The object is accessible only from within the method of creation. In most cases, the compiler
 * can eliminate the object and replace its fields by scalar variables [...]. Objects with this
 * escape level are called method-local." (Kotzmann and Moessenboeck 2005)
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
 * "The object escapes the current method but not the current thread, e.g. because it is passed
 * to a callee which does not let the argument escape. It is possible to allocate the object on
 * the stack and eliminate any synchronization on it. Objects with this escape level are called
 * thread-local." (Kotzmann and Moessenboeck 2005)
 */
case object ArgEscape extends EscapeProperty {
    final val isRefineable = false
}

/**
 *
 */
case object ConditionallyMethodEscape extends EscapeProperty {
    final val isRefineable: Boolean = true
}

/**
 *
 * "The object escapes globally, typically because it is assigned to a static variable or to a
 * field of a heap object. The object must be allocated on the heap, because it can be referenced
 * by other threads and methods." (Kotzmann and Moessenboeck 2005)
 */
case object GlobalEscape extends EscapeProperty {
    final val isRefineable = false
}

