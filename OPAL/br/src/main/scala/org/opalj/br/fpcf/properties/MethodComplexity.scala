/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

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
 * @param value The estimated complexity of a specific method ([0...`Int.MaxMavlue`])
 *
 * @author Michael Eichberg
 */
case class MethodComplexity(
        value: Int
) extends Property
    with MethodComplexityPropertyMetaInformation {

    assert(value >= 0)

    final def key = MethodComplexity.key

}

object MethodComplexity extends MethodComplexityPropertyMetaInformation {

    final val TooComplex = MethodComplexity(Int.MaxValue)

    final val key = PropertyKey.create[Method, MethodComplexity]("MethodComplexity", TooComplex)

}
