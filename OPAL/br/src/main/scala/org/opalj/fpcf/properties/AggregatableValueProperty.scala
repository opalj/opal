/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

/**
 * A trait to conveniently implement properties that need aggregated values.
 * Provides conversion between aggregated and individual variants.
 *
 * @tparam T Type of the property
 *
 * @author Dominik Helm
 */
trait AggregatableValueProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends Property {
    type self

    def meet(other: self): self
}

trait IndividualProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends AggregatableValueProperty[S, T] {
    override type self = S

    val aggregatedProperty: T
}

trait AggregatedProperty[S <: IndividualProperty[S, T], T <: AggregatedProperty[S, T]]
    extends AggregatableValueProperty[S, T] {
    override type self = T

    val individualProperty: S

    override def meet(other: T): T =
        other.individualProperty.meet(this.individualProperty).aggregatedProperty
}
