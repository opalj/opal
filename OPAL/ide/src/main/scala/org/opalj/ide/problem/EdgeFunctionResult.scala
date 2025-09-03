/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package problem

import scala.collection

import org.opalj.fpcf.SomeEOptionP

/**
 * Interface for encapsulating different states of edge functions.
 *
 * @author Robin Körkemeier
 */
trait EdgeFunctionResult[Value <: IDEValue]

/**
 * Represent an edge function that is final.
 *
 * @author Robin Körkemeier
 */
case class FinalEdgeFunction[Value <: IDEValue](edgeFunction: EdgeFunction[Value]) extends EdgeFunctionResult[Value]

/**
 * Represent an interim edge function that may change when the result of one of the dependees changes.
 *
 * @param interimEdgeFunction an interim edge function to use until new results are present (has to be an upper bound of
 *                            the final edge function)
 *
 * @author Robin Körkemeier
 */
case class InterimEdgeFunction[Value <: IDEValue](
    interimEdgeFunction: EdgeFunction[Value],
    dependees:           collection.Set[SomeEOptionP]
) extends EdgeFunctionResult[Value]
