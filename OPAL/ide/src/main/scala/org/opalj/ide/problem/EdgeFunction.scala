/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.problem

/**
 * Interface representing IDE edge functions
 */
trait EdgeFunction[Value <: IDEValue] {
    /**
     * Compute the value of the edge function
     * @param sourceValue the incoming parameter value
     */
    def compute(sourceValue: Value): Value

    /**
     * Compose two edge functions
     * @param secondEdgeFunction the edge function that is applied after this one
     * @return an edge function computing the same values as first applying this edge function and then applying the
     *         result to the second edge function
     */
    def composeWith(secondEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value]

    /**
     * Combine two edge functions via meet semantics
     */
    def meetWith(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value]

    /**
     * Check whether two edge functions are equal (s.t. they produce the same result for same source values)
     */
    def equalTo(otherEdgeFunction: EdgeFunction[Value]): Boolean
}

/**
 * Special edge function representing an identity edge function
 */
case class IdentityEdgeFunction[Value <: IDEValue]() extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        sourceValue

    override def composeWith(secondEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] =
        secondEdgeFunction

    override def meetWith(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] = {
        if (otherEdgeFunction.equalTo(this) || otherEdgeFunction.isInstanceOf[AllTopEdgeFunction[Value]]) {
            this
        } else if (otherEdgeFunction.isInstanceOf[AllBottomEdgeFunction[Value]]) {
            otherEdgeFunction
        } else {
            otherEdgeFunction.meetWith(this)
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        otherEdgeFunction == this || otherEdgeFunction.isInstanceOf[IdentityEdgeFunction[Value]]
}

/**
 * Special edge function representing an edge function where all source values evaluate to the top element. Implementing
 * [[composeWith]] is left to the user, as it requires knowledge of the other possible edge functions.
 */
abstract case class AllTopEdgeFunction[Value <: IDEValue](private val top: Value) extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        top

    override def meetWith(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] =
        otherEdgeFunction

    override def equalTo(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        otherEdgeFunction == this ||
            otherEdgeFunction.isInstanceOf[AllTopEdgeFunction[Value]] &&
            otherEdgeFunction.asInstanceOf[AllTopEdgeFunction[Value]].top == top
}

/**
 * Special edge function representing an edge function where all source values evaluate to the bottom element.
 * Implementing [[composeWith]] is left to the user, as it requires knowledge of the other possible edge functions.
 */
abstract case class AllBottomEdgeFunction[Value <: IDEValue](private val bottom: Value) extends EdgeFunction[Value] {
    override def compute(sourceValue: Value): Value =
        bottom

    override def meetWith(otherEdgeFunction: EdgeFunction[Value]): EdgeFunction[Value] =
        this

    override def equalTo(otherEdgeFunction: EdgeFunction[Value]): Boolean =
        otherEdgeFunction == this ||
            otherEdgeFunction.isInstanceOf[AllBottomEdgeFunction[Value]] &&
            otherEdgeFunction.asInstanceOf[AllBottomEdgeFunction[Value]].bottom == bottom
}
