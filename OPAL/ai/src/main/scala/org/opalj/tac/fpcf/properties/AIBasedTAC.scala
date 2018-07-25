/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.EPS
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyIsNotComputedByAnyAnalysis
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.value.KnownTypedValue

sealed trait AIBasedTACPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = AIBasedTAC

}

/**
 * Encapsulates the (intermediate) three-address code of a method.
 *
 * @note The underlying domain that is used to create the three-address code is determined by
 *       the scheduled analysis.
 *       If no analysis is scheduled, the [[PrimitiveTACAIDomain]] will be used.
 *
 * @author Michael Eichberg
 */
sealed trait AIBasedTAC extends Property with AIBasedTACPropertyMetaInformation {

    /**
     * Returns the key used by all `BaseAIResult` properties.
     */
    final def key = AIBasedTAC.key

    /**
     * @return The three-address code if the method is reachable; `None` otherwise.
     */
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]]
}

/**
 * Models the TOP of the lattice. Used iff the method is not reachable, which generally requires
 * the computation of a call graph.
 */
case object NoAIBasedTAC extends AIBasedTAC {
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]] = None
}

case class AnAIBasedTAC(
        theTAC: TACode[TACMethodParameter, DUVar[KnownTypedValue]]
) extends AIBasedTAC {
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]] = Some(theTAC)
}

/**
 * Common constants use by all [[AIBasedTAC]] properties associated with methods.
 */
object AIBasedTAC extends AIBasedTACPropertyMetaInformation {

    /**
     * The key associated with every [[AIBasedTAC]] property.
     */
    final val key: PropertyKey[AIBasedTAC] = PropertyKey.create[Method, AIBasedTAC](
        "org.opalj.tac.fpcf.properties.AIBasedTAC",
        // fallback property computation...
        (ps: PropertyStore, r: FallbackReason, m: Method) ⇒ {
            r match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoAIBasedTAC

                case PropertyIsNotComputedByAnyAnalysis ⇒
                    val p = ps.context(classOf[SomeProject])
                    val d = new PrimitiveTACAIDomain(p.classHierarchy, m)
                    val taCode = TACAI(p, m)(d)
                    AnAIBasedTAC(
                        // the following cast is safe - see TACode for details
                        // TODO Get rid of nasty type checks/casts related to TACode once we use TypeSafeArray in TACode.. (here and elsewhere)
                        taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[KnownTypedValue]]]
                    )
            }
        }: AIBasedTAC,
        // cycle resolution strategy...
        (_: PropertyStore, eps: EPS[Method, AIBasedTAC]) ⇒ eps.ub,
        // fast-track property computation...
        (_: PropertyStore, _: Method) ⇒ None
    )
}
