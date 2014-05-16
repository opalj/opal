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
package invokedynamic

import scala.language.existentials

import br._
import br.instructions.INVOKEDYNAMIC


/**
 * Represents the result of the resolution of an `invokedynamic` instruction.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
sealed trait ResolutionResult {

    /**
     * The original instruction.
     */
    val instruction: INVOKEDYNAMIC

    /**
     * Targets identified within the project.
     */
    def concreteTargets: Traversable[ClassMember]

    /**
     * All identified targets. (It may be possible that we identify a method
     * that will be called/a field that may be accessed, but that field/method
     * is currently not available – not part of the analyzed code base – and, hence,
     * we can in general only return virtual class members.)
     */
    def allTargets: Traversable[VirtualClassMember]
}

/**
 * The result to be returned when the resolution process was completed and it was
 * not possible to identify a single target.
 *
 * @author Arne Lottmann
 */
case class ResolutionFailed(
        override val instruction: INVOKEDYNAMIC) extends ResolutionResult {

    def concreteTargets: Traversable[ClassMember] = Traversable.empty

    def allTargets: Traversable[VirtualClassMember] = Traversable.empty
}
