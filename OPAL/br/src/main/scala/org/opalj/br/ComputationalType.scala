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
package br

import scala.collection.SortedSet

import org.opalj.collection.UID
import org.opalj.collection.immutable.UIDSet

import org.opalj.br.instructions._

/**
 * The computational type of a value on the operand stack.
 *
 * (cf. JVM Spec. 2.11.1 Types and the Java Virtual Machine).
 */
sealed abstract class ComputationalType(
        computationTypeCategory: ComputationalTypeCategory) {

    def operandSize = computationTypeCategory.operandSize

    def isComputationalTypeReturnAddress: Boolean

    def category = computationTypeCategory.id

}
case object ComputationalTypeInt
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeFloat
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeReference
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeReturnAddress
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = true
}
case object ComputationalTypeLong
        extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeDouble
        extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
