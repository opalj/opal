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
package seq

// NOTE
// For collaboratively computed properties isFinal maybe false, but we do not have dependees!
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
        throw new ClassCastException(s"$this is not an IntermediatePropertyValue")
    }

    def toEPS[E <: Entity](e: E): Option[EPS[E, Property]] = {
        if (ub == null || ub == PropertyIsLazilyComputed)
            None
        else
            Some(EPS(e, lb, ub))
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

    def this(epk: SomeEPK, c: OnUpdateContinuation) {
        this(null, null, Map(epk → c), Nil)
    }

    final override def isFinal: Boolean = {
        val ub = this.ub
        ub != null && ub != PropertyIsLazilyComputed && ub == lb
    }

    final override def asIntermediate: IntermediatePropertyValue = this
}

private[seq] final class FinalPropertyValue(val ub: Property) extends PropertyValue {
    assert(ub != PropertyIsLazilyComputed)
    assert(ub != null)
    final override def lb: Property = ub
    final override def dependers: Map[SomeEPK, OnUpdateContinuation] = Map.empty
    final override def dependees: Traversable[SomeEOptionP] = Nil
    final override def isFinal: Boolean = true
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
            assert(dependees.isEmpty)
            new FinalPropertyValue(ub)
        } else {
            new IntermediatePropertyValue(lb, ub, Map.empty, dependees)
        }
    }

}
