/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package problem

/**
 * Interface representing IDE edge functions.
 *
 * @author Robin Körkemeier
 */
trait EdgeFunction[Value <: IDEValue] {
    /**
     * Compute the value of the edge function
     *
     * @param sourceValue the incoming parameter value
     */
    def compute(sourceValue: Value): Value

    /**
     * Compose two edge functions
     *
     * @param secondEdgeFunction the edge function that is applied after this one
     * @return an edge function computing the same values as first applying this edge function and then applying the
     *         result to the second edge function
     */
    def composeWith(secondEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value]

    /**
     * Combine two edge functions via meet semantics
     */
    def meet(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value]

    /**
     * Check whether two edge functions are equal (s.t. they produce the same result for same source values)
     */
    def equals(otherEdgeFunction: EdgeFunction[Value]): Boolean
}

/**
 * Special edge function representing an identity edge function.
 *
 * @author Robin Körkemeier
 */
case class IdentityEdgeFunction[Value <: IDEValue]() extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        sourceValue

    override def composeWith(secondEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] =
        secondEdgeFunction

    override def meet(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] = {
        if (otherEdgeFunction.equals(this)) {
            this
        } else {
            otherEdgeFunction.meet(this)
        }
    }

    override def equals(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case _: IdentityEdgeFunction[Value] => true
                case _                              => false
            })
}

/**
 * Special edge function representing an edge function where all source values evaluate to the top element. Implementing
 * [[composeWith]] is left to the user, as it requires knowledge of the other possible edge functions.
 *
 * @author Robin Körkemeier
 */
abstract case class AllTopEdgeFunction[Value <: IDEValue](private val top: Value) extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        top

    override def meet(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] = {
        otherEdgeFunction match {
            case _: AllTopEdgeFunction[Value] => this
            case _                            => otherEdgeFunction
        }
    }

    override def equals(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case AllTopEdgeFunction(top2) => top == top2
                case _                        => false
            })
}

/**
 * Special edge function representing an edge function where all source values evaluate to the bottom element.
 * Implementing [[composeWith]] is left to the user, as it requires knowledge of the other possible edge functions.
 *
 * @author Robin Körkemeier
 */
abstract case class AllBottomEdgeFunction[Value <: IDEValue](private val bottom: Value) extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        bottom

    override def meet(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] =
        this

    override def equals(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case AllBottomEdgeFunction(bottom2) => bottom == bottom2
                case _                              => false
            })
}
