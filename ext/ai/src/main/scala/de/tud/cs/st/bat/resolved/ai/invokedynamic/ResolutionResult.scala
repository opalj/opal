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
package de.tud.cs.st
package bat
package resolved
package ai
package invokedynamic

import instructions.INVOKEDYNAMIC

import language.existentials 

/**
 * Base trait that represents the result of an [[InvokedynamicResolver]]'s resolution run.
 * 
 * @author Arne Lottmann
 */
sealed trait ResolutionResult {
    val instruction: INVOKEDYNAMIC
}

/**
 * The result to be returned when there is a single, definitive match for the given 
 * instruction.
 * 
 * @author Arne Lottmann
 */
case class SingleResult(
    val matched: ClassMember,
    override val instruction: INVOKEDYNAMIC)
        extends ResolutionResult

/**
 * The result to be returned when there are multiple possible matches '''if''' they share
 * an inheritance hierarchy '''and''' if it is possible to determine one type among the
 * possible matches that is a super type of all others.
 * 
 * @param conservativeMatch the best guarantee for a match that can be made under the
 *  circumstances; i.e. this `ClassMember` belongs to the type at the top of the 
 *  hierarchy.
 *  
 * @param alternativeMatches the remaining candidates within the hierarchy
 *  (`conservativeMatch` should not be a part of this `Set`.)
 * 
 * @author Arne Lottmann
 */
case class InheritanceResult(
    val conservativeMatch: ClassMember,
    val alternativeMatches: Set[_ <: ClassMember],
    override val instruction: INVOKEDYNAMIC)
        extends ResolutionResult

/**
 * The result to be returned when there are multiple possible matches for which it is
 * impossible to determine one common super type.
 * 
 * @see [[InheritanceResult]]
 * 
 * @author Arne Lottmann
 */
case class MultipleResults(
    val possibleMatches: Set[_ <: ClassMember],
    override val instruction: INVOKEDYNAMIC)
        extends ResolutionResult

/**
 * The result to be returned when the resolution process was completed without finding 
 * any `ClassMember` that could match the given instruction.
 * 
 * @author Arne Lottmann
 */
case class ResolutionFailed(override val instruction: INVOKEDYNAMIC) extends ResolutionResult
