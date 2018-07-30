/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

/**
 * Enapculates the extension of a specific property for a specific entity.
 *
 * @note   For collaboratively computed properties isFinal maybe false, but we do not
 *         have dependees!
 * @author Michael Eichberg
 */
private[seq] sealed abstract class PropertyValue {
    // lb/ub are :
    //   - null if some analyses depend on it, but no lazy computation is scheduled
    //   - PropertyIsLazilyComputed if the computation is scheduled (to avoid rescheduling)
    //   - a concrete Property.
    def lb: Property
    def ub: Property

    //
    // Both of the following collections are maintained eagerly in the
    // the sense that, if an update happens, the on update
    // continuation will directly be scheduled and the corresponding
    // collections will be cleared respectively.
    //
    // Those who are interested in this property;
    // the keys are those EPKs with a dependency on this one:
    def dependers: Map[SomeEPK, OnUpdateContinuation]

    // The properties on which this property depends;
    // required to remove the onUpdateContinuation for this
    // property from the dependers maps of the dependees.
    // Note, dependees can even be empty for non-final properties
    // in case of collaboratively computed properties OR if a task
    // which computes the next value is already scheduled!
    def dependees: Traversable[SomeEOptionP]

    def isFinal: Boolean

    def asIntermediate: IntermediatePropertyValue = {
        throw new ClassCastException(s"$this is not an IntermediatePropertyValue");
    }

    def toEPS[E <: Entity](e: E): Option[EPS[E, Property]] = {
        if (ub == null || ub == PropertyIsLazilyComputed)
            None
        else
            Some(EPS(e, lb, ub))
    }

    private[seq] def toEPSUnsafe[E <: Entity, P <: Property](e: E): Option[EPS[E, P]] = {
        if (ub == null || ub == PropertyIsLazilyComputed)
            None
        else
            Some(EPS[E, P](e, lb.asInstanceOf[P], ub.asInstanceOf[P]))
    }

    override def toString: String = {
        (if (isFinal) "Final" else "Intermediate")+
            "PropertyValue("+
            "\n\tlb="+lb+
            "\n\tub="+ub+
            "\n\t#dependers="+dependers.size + dependers.keys.mkString(";dependers={", ", ", "}")+
            "\n\t#dependees="+dependees.size + dependees.mkString(";dependees={", ", ", "}")+
            "\n)"
    }
}

private[seq] final class IntermediatePropertyValue(
        var lb:        Property,
        var ub:        Property,
        var dependers: Map[SomeEPK, OnUpdateContinuation],
        var dependees: Traversable[SomeEOptionP]
) extends PropertyValue {

    assert(ub != lb || ub == PropertyIsLazilyComputed || ub == null)

    def this(dependerEPK: SomeEPK, c: OnUpdateContinuation) {
        this(null, null, Map(dependerEPK → c), Nil)
    }

    override def isFinal: Boolean = {
        val ub = this.ub
        ub != null && ub != PropertyIsLazilyComputed && ub == lb
    }

    override def asIntermediate: IntermediatePropertyValue = this
}

object IntermediatePropertyValue {
    def lazilyComputed(dependerEPK: SomeEPK, c: OnUpdateContinuation): PropertyValue = {
        new IntermediatePropertyValue(
            PropertyIsLazilyComputed,
            PropertyIsLazilyComputed,
            Map(dependerEPK → c),
            Nil
        )
    }
}

private[seq] final class FinalPropertyValue(val ub: Property) extends PropertyValue {
    assert(ub != PropertyIsLazilyComputed, "ub is lazily computed")
    assert(ub != null, "ub is null")
    override def lb: Property = ub
    override def dependers: Map[SomeEPK, OnUpdateContinuation] = Map.empty
    override def dependees: Traversable[SomeEOptionP] = Nil
    override def isFinal: Boolean = true
}

private[seq] object PropertyValue {

    def lazilyComputed: PropertyValue = {
        new IntermediatePropertyValue(
            PropertyIsLazilyComputed,
            PropertyIsLazilyComputed,
            Map.empty,
            Nil
        )
    }

    def apply(
        lb:        Property,
        ub:        Property,
        dependees: Traversable[SomeEOptionP]
    ): PropertyValue = {
        if (lb == ub && ub != PropertyIsLazilyComputed) {
            new FinalPropertyValue(ub)
        } else {
            new IntermediatePropertyValue(lb, ub, Map.empty, dependees)
        }
    }

}
