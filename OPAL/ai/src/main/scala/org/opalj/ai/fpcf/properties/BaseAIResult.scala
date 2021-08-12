/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyIsNotComputedByAnyAnalysis
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

sealed trait BaseAIResultPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = BaseAIResult

}

/**
 * Encapsulates the (intermediate) result of the abstract interpretation of a method.
 *
 * As in case of the other properties, the upper bound represents the most precise result and
 * the lower bound the sound over approximation. Initially, the upper bound is the special
 * value [[NoAIResult]] and identifies the case where the method is not reached at all. The lower
 * bound generally models the sound over-approximation of all paths that are potentially taken.
 *
 * The upper bound can only meaningfully be computed in combination with a call graph.
 *
 * @author Michael Eichberg
 */
sealed trait BaseAIResult extends Property with BaseAIResultPropertyMetaInformation {

    /**
     * Returns the key used by all `BaseAIResult` properties.
     */
    final def key: PropertyKey[BaseAIResult] = BaseAIResult.key

    def aiResult: Option[AIResult]
}

case object NoAIResult extends BaseAIResult {
    def aiResult: Option[AIResult] = None
}

case class AnAIResult(theAIResult: AIResult) extends BaseAIResult {
    def aiResult: Option[AIResult] = Some(theAIResult)
}

/**
 * Common constants used by all [[BaseAIResult]] properties associated with methods.
 */
object BaseAIResult extends BaseAIResultPropertyMetaInformation {

    /**
     * The key associated with every [[BaseAIResult]] property.
     */
    final val key: PropertyKey[BaseAIResult] = PropertyKey.create[Method, BaseAIResult](
        "opalj.BaseAIResult",
        (ps: PropertyStore, r: FallbackReason, m: Method) => {
            r match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                    NoAIResult

                case PropertyIsNotComputedByAnyAnalysis =>
                    // we may still have requirements on the domain that we are going to use...
                    val p = ps.context(classOf[SomeProject])
                    AnAIResult(p.get(AIDomainFactoryKey)(m))
            }
        }: BaseAIResult
    )
}
