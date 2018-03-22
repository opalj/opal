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
package org.opalj.fpcf

// TODO REname to PropertyState
/**
 * The different types of updates distinguished by the fixed-point computations framework.
 *
 * @author Michael Eichberg
 */
sealed abstract class UpdateType(val name: String) {
    val id: Int

    def isIntermediateUpdate: Boolean
    def isPhaseFinalUpdate: Boolean
    def isFinalUpdate: Boolean
}

/**
 * The result is just an intermediate result that may be refined in the future.
 *
 * @note Refinable results are - downstream - only intermediate updates.
 */
// TODO Rename to IntermediateProperty
case object IntermediateUpdate extends UpdateType("Intermediate Update") {
    final val id = 1
    final override def isIntermediateUpdate: Boolean = true
    final override def isPhaseFinalUpdate: Boolean = false
    final override def isFinalUpdate: Boolean = false
}

// TODO Rename to PhaseFinalProperty
case object PhaseFinalUpdate extends UpdateType("Phase Final Update") {
    final val id = 3
    final override def isIntermediateUpdate: Boolean = false
    final override def isPhaseFinalUpdate: Boolean = true
    final override def isFinalUpdate: Boolean = false
}

/**
 * The result is the final result and was computed using other information.
 */
// TODO Rename to FinalProperty
case object FinalUpdate extends UpdateType("Final Update") {
    final val id = 3
    final override def isIntermediateUpdate: Boolean = false
    final override def isPhaseFinalUpdate: Boolean = false
    final override def isFinalUpdate: Boolean = true
}

