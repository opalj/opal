/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package problem

/**
 * Interface representing IDE edge functions.
 *
 * @author Robin Körkemeier
 */
trait EdgeFunction[+Value <: IDEValue] {
    /**
     * Compute the value of the edge function
     *
     * @param sourceValue the incoming parameter value
     */
    def compute[V >: Value](sourceValue: V): V

    /**
     * Compose two edge functions
     *
     * @param secondEdgeFunction the edge function that is applied after this one
     * @return an edge function computing the same values as first applying this edge function and then applying the
     *         result to the second edge function
     */
    def composeWith[V >: Value <: IDEValue](secondEdgeFunction: EdgeFunction[V]): EdgeFunction[V]

    /**
     * Combine two edge functions via meet semantics
     */
    def meet[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): EdgeFunction[V]

    /**
     * Check whether two edge functions are equal (s.t. they produce the same result for same source values)
     */
    def equals[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): Boolean
}

/**
 * Special edge function representing an identity edge function.
 *
 * @author Robin Körkemeier
 */
case object IdentityEdgeFunction extends EdgeFunction[Nothing] {
    override def compute[V >: Nothing](sourceValue: V): V =
        sourceValue

    override def composeWith[V >: Nothing <: IDEValue](secondEdgeFunction: EdgeFunction[V]): EdgeFunction[V] =
        secondEdgeFunction

    override def meet[V >: Nothing <: IDEValue](otherEdgeFunction: EdgeFunction[V]): EdgeFunction[V] = {
        if (otherEdgeFunction.equals(this)) {
            this
        } else {
            otherEdgeFunction.meet(this)
        }
    }

    override def equals[V >: Nothing <: IDEValue](otherEdgeFunction: EdgeFunction[V]): Boolean =
        otherEdgeFunction eq this
}

/**
 * Special edge function representing an edge function where all source values evaluate to the top element. Implementing
 * [[composeWith]] is left to the user, as it requires knowledge of the other possible edge functions.
 *
 * @author Robin Körkemeier
 */
abstract case class AllTopEdgeFunction[Value <: IDEValue](private val top: Value) extends EdgeFunction[Value] {
    override def compute[V >: Value](sourceValue: V): V =
        top

    override def meet[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): EdgeFunction[V] = {
        otherEdgeFunction match {
            case _: AllTopEdgeFunction[V @unchecked] => this
            case _                                   => otherEdgeFunction
        }
    }

    override def equals[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): Boolean =
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
    override def compute[V >: Value](sourceValue: V): V =
        bottom

    override def meet[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): EdgeFunction[V] =
        this

    override def equals[V >: Value <: IDEValue](otherEdgeFunction: EdgeFunction[V]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case AllBottomEdgeFunction(bottom2) => bottom == bottom2
                case _                              => false
            })
}
