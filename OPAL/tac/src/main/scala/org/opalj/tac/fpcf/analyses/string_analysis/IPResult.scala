/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation

/**
 * A result of an interpretation of some def site using some [[interpretation.InterpretationHandler]].
 * Essentially a simplified version of [[org.opalj.fpcf.EOptionP]] that has been adapted to the needs of string analyses.
 *
 * @author Maximilian Rüsch
 */
trait IPResult {
    def isNoResult: Boolean

    def isFinal: Boolean
    final def isRefinable: Boolean = !isFinal

    def asFinal: FinalIPResult

    def sciOpt: Option[StringConstancyInformation]
}

/**
 * Indicates that the def site is not relevant to the interpretation of the analyses.
 *
 * @note Since the def site is not relevant, we are able to return a final result with the neutral element when needed.
 *       Interpreters handling this result type should either convert it to
 *       [[StringConstancyInformation.getNeutralElement]] or preserve it.
 */
object NoIPResult extends IPResult {
    override def isNoResult: Boolean = true

    override def isFinal: Boolean = true

    override def asFinal: FinalIPResult = FinalIPResult(StringConstancyInformation.getNeutralElement)
    override def sciOpt: Option[StringConstancyInformation] = None
}

trait SomeIPResult extends IPResult {
    override final def isNoResult: Boolean = false
}

object EmptyIPResult extends SomeIPResult {
    def isFinal = false
    override def asFinal: FinalIPResult = throw new UnsupportedOperationException()

    override def sciOpt: Option[StringConstancyInformation] = None
}

trait ValueIPResult extends SomeIPResult {
    override final def sciOpt: Option[StringConstancyInformation] = Some(sci)

    def sci: StringConstancyInformation
}

object ValueIPResult {
    def unapply(valueIPResult: ValueIPResult): Some[StringConstancyInformation] = Some(valueIPResult.sci)
}

case class FinalIPResult(override val sci: StringConstancyInformation) extends ValueIPResult {
    override final def isFinal: Boolean = true
    override def asFinal: FinalIPResult = this
}

object FinalIPResult {
    def nullElement = new FinalIPResult(StringConstancyInformation.getNullElement)
    def lb = new FinalIPResult(StringConstancyInformation.lb)
}

case class InterimIPResult(override val sci: StringConstancyInformation) extends ValueIPResult {
    override final def isFinal = false
    override def asFinal: FinalIPResult = throw new UnsupportedOperationException()
}

object InterimIPResult {
    def lb = new InterimIPResult(StringConstancyInformation.lb)
}
