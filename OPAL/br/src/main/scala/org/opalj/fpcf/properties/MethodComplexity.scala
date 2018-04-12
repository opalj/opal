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

import org.opalj.br.Method

sealed trait MethodComplexityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = MethodComplexity
}

/**
 * An upper bound for the maximum number of instructions that need to be evaluated
 * when the method is executed/interpreted.
 * Using this property enables other analyses to make a decision whether to "inline" method calls
 * or not.
 * For example, to improve the precision of an analysis it may be very useful to inline short
 * methods. Given the following code:
 * {{{
 *  def abs(i : Int) : Int = {
 *      if(i == Int.MinValue) Int.MaxValue // COND + RETURN
 *      else if(i < 0) -i // COND + RETURN
 *      else i // RETURN
 *  } // COMPLEXITY (BASED ON SOURCE CODE): 5
 *
 *  def ratio(i : Int, j : Int) : Int = {
 *      abs(i) / abs(j) // HERE, when we do not inline abs calls, we have no idea about the final
 *                      // result; when we inline the abs calls, we can compute that the returned
 *                      // value will be positive or that the method throws a
 *                      // `DivisionByZeroException`.
 *  }
 * }}}
 *
 * In general, the control flow graph is analyzed to compute an upper bound for the number
 * of evaluated instructions; as far as (easily) possible, loops are conceptually unrolled. If
 * the uppper bound could not be determined, the method is rated as being maximally complex.
 *
 * The complexity of called methods is generally not taken into account. However, invoke
 * instructions generally have a higher complexity than all other instructions to account for the
 * fact that method calls are more expensive than all other types of instructions.
 *
 * If an upper bound of a method's complexity cannot be estimated, the method will have
 * `Int.MaxValue` complexity.
 *
 * @param value The estimated complexity of a specific method ([[0...`Int.MaxMavlue`]])
 *
 * @author Michael Eichberg
 */
case class MethodComplexity(
        value: Int
) extends Property with MethodComplexityPropertyMetaInformation {

    assert(value >= 0)

    final def key = MethodComplexity.key

}

object MethodComplexity extends MethodComplexityPropertyMetaInformation {

    final val TooComplex = MethodComplexity(Int.MaxValue)

    final val key = PropertyKey.create[Method, MethodComplexity]("MethodComplexity", TooComplex)

}
