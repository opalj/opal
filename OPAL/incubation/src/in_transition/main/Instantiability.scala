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

sealed trait InstantiabilityPropertyMetaInformation extends PropertyMetaInformation {
    type Self = Instantiability
}

/**
 * This is a common trait for all Instantiability properties which can be emitted to the
 * PropertyStore. It describes the instantibility of a class entity.
 */
sealed trait Instantiability extends Property with InstantiabilityPropertyMetaInformation {
    final def key = Instantiability.key // All instances have to share the SAME key!
}

/**
 * A companion object for the Instantiability trait. It holds the key, which is shared by
 * all properties derived from the Instantiability property, as well as it defines defines
 * the (sound) fall back if the property is not computed but requested by another analysis.
 */
object Instantiability extends InstantiabilityPropertyMetaInformation {
    final val key = PropertyKey.create[Instantiability]("Instantiability", Instantiable)
}

/**
 * NotInstantiable should be used for not instantiable classes.
 *
 * @example
 * {{{
 * public class Foo {
 *      private Foo(){}
 * }
 * }}}
 *
 * Foo is not instantiable because it can not be instantiated except
 * by Foo itself which does never call the private constructor.
 */
case object NotInstantiable extends Instantiability { final val isRefinable: Boolean = false }

/**
 * Should be assigned to classes which can be instantiated or are instantiated.
 */
case object Instantiable extends Instantiability { final val isRefinable: Boolean = false }

/**
 * Classes that are MaybeInstantiable lack of information. E.g., we don't know whether it is
 * instantiable or not.
 */
case object MaybeInstantiable extends Instantiability { final val isRefinable: Boolean = true }
