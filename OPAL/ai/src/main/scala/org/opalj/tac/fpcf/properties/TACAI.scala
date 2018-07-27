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
import org.opalj.tac.{TACAI ⇒ TACAIFactory}

sealed trait TACAIPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = TACAI

}

/**
 * Encapsulates the (intermediate) three-address code of a method.
 *
 * @note The underlying domain that is used to create the three-address code is determined by
 *       the scheduled analysis.
 *
 * @author Michael Eichberg
 */
sealed trait TACAI extends Property with TACAIPropertyMetaInformation {

    /**
     * Returns the key used by all `BaseAIResult` properties.
     */
    final def key = TACAI.key

    /**
     * @return The three-address code if the method is reachable; `None` otherwise.
     */
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]]
}

/**
 * Models the TOP of the lattice. Used iff the method is not reachable, which generally requires
 * the computation of a call graph.
 */
case object NoTACAI extends TACAI {
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]] = None
}

case class TheTACAI(
        theTAC: TACode[TACMethodParameter, DUVar[KnownTypedValue]]
) extends TACAI {
    def tac: Option[TACode[TACMethodParameter, DUVar[KnownTypedValue]]] = Some(theTAC)
}

/**
 * Common constants use by all [[TACAI]] properties associated with methods.
 */
object TACAI extends TACAIPropertyMetaInformation {

    /**
     * The key associated with every [[TACAI]] property.
     */
    final val key: PropertyKey[TACAI] = PropertyKey.create[Method, TACAI](
        "org.opalj.tac.fpcf.properties.AIBasedTAC",
        // fallback property computation...
        (ps: PropertyStore, r: FallbackReason, m: Method) ⇒ {
            r match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoTACAI

                case PropertyIsNotComputedByAnyAnalysis ⇒
                    val p = ps.context(classOf[SomeProject])
                    val d = new PrimitiveTACAIDomain(p.classHierarchy, m)
                    val taCode = TACAIFactory(p, m)(d)
                    TheTACAI(
                        // the following cast is safe - see TACode for details
                        // IMPROVE Get rid of nasty type checks/casts related to TACode once we use ConstCovariantArray in TACode.. (here and elsewhere)
                        taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[KnownTypedValue]]]
                    )
            }
        }: TACAI,
        // cycle resolution strategy...
        (_: PropertyStore, eps: EPS[Method, TACAI]) ⇒ eps.ub,
        // fast-track property computation...
        (_: PropertyStore, _: Method) ⇒ None
    )
}
