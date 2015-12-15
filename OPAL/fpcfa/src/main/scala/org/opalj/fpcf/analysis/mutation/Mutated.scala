/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package mutation

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * Describes the behavior of the program w.r.t. mutating the fields of a class.
 *
 * A field is considered as ''effectively'' final if the field is:
 *  - actually final or
 *  - if the private field is only set by a constructor or a private helper method that is only
 *    called by a constructor.
 *  - if the field is set at most once to a value that is not the default value (`0`, `0l`, `0f`,
 *    `0d`, `null`) (lazy initialization) and the value is NOT depending on mutable information
 *    (in particular method parameters).
 *
 * @author Michael Eichberg
 */
sealed trait Mutated extends Property {
    final def key: PropertyKey = Mutated.key // All instances have to share the SAME key!
}

object Mutated extends PropertyMetaInformation {

    final val key: PropertyKey = PropertyKey.create("Mutability", NonFinalByLackOfInformation)

}

sealed trait Final extends Mutated {
    final val isRefineable: Boolean = false
    val byDefinition: Boolean
}

case object EffectivelyFinal extends Final { final val byDefinition = false }

case object DeclaredFinal extends Final { final val byDefinition = true }

sealed trait NonFinal extends Mutated {
    final val isRefineable: Boolean = false
    val byReason: String
}

case object NonFinalByAnalysis extends NonFinal {
    final val byReason = "determined by analysis"
}

case object NonFinalByLackOfInformation extends NonFinal {
    final val byReason = "some required information was not available"
}