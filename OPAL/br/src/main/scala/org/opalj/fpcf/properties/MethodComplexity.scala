package org.opalj
package fpcf
package properties

sealed trait MethodComplexityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = MethodComplexity
}

/**
 *
 * @param value The estimated complexity of a specific method.
 */
case class MethodComplexity(value: Int) extends Property with MethodComplexityPropertyMetaInformation {

    assert(value >= 0)

    final def key = MethodComplexity.key // All instances have to share the SAME key!

    final val isRefineable = false

}

object MethodComplexity extends MethodComplexityPropertyMetaInformation {

    final val TooComplex = MethodComplexity(Int.MaxValue)

    final val key = PropertyKey.create[MethodComplexity]("MethodComplexity", TooComplex)

}