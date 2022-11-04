/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

/**
 * In case that the arraylength is just an integer value, the value is refined to the
 * range [0...Int.MaxValue].
 *
 * @author Michael Eichberg
 */
trait MaxArrayLengthRefinement extends l0.TypeLevelReferenceValues {
    domain: Domain with IntegerRangeValues =>

    abstract override def arraylength(
        pc:       Int,
        arrayref: DomainValue
    ): Computation[DomainValue, ExceptionValue] = {
        val length = super.arraylength(pc, arrayref)
        if (length.hasResult) {
            length.result match {
                case _: AnIntegerValueLike => length.updateResult(IntegerRange(0, Int.MaxValue))
                case _                     => length
            }
        } else {
            // if the array is null..
            length
        }
    }
}
