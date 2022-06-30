/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyIsNotComputedByAnyAnalysis
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.tac.{TACAI => TACAIFactory}

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

    final def key: PropertyKey[TACAI] = TACAI.key

    /**
     * @return The three-address code if the method is reachable; `None` otherwise.
     */
    def tac: Option[TACode[TACMethodParameter, DUVar[ValueInformation]]]
}

/**
 * Models the top of the lattice; used iff the method is not reachable, which generally requires
 * the computation of a call graph.
 */
case object NoTACAI extends TACAI {
    def tac: Option[TACode[TACMethodParameter, DUVar[ValueInformation]]] = None
}

case class TheTACAI(
        theTAC: TACode[TACMethodParameter, DUVar[ValueInformation]]
) extends TACAI {
    def tac: Option[TACode[TACMethodParameter, DUVar[ValueInformation]]] = Some(theTAC)
}

/**
 * Common constants use by all [[TACAI]] properties associated with methods.
 */
object TACAI extends TACAIPropertyMetaInformation {

    /**
     * The key associated with every [[TACAI]] property.
     */
    final val key: PropertyKey[TACAI] = PropertyKey.create[Method, TACAI](
        "opalj.TACAI",
        (ps: PropertyStore, r: FallbackReason, m: Method) => {
            r match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                    NoTACAI

                case PropertyIsNotComputedByAnyAnalysis =>
                    val p = ps.context(classOf[SomeProject])
                    val d = new PrimitiveTACAIDomain(p.classHierarchy, m)
                    val taCode = TACAIFactory(p, m)(d)
                    TheTACAI(
                        // the following cast is safe - see TACode for details
                        taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[ValueInformation]]]
                    )
            }
        }: TACAI
    )
}
