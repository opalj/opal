/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS

/**
 * A result of an interpretation of some def site using some [[interpretation.InterpretationHandler]].
 * Essentially a simplified version of [[org.opalj.fpcf.EOptionP]] that has been adapted to the needs of string analyses.
 *
 * @author Maximilian Rüsch
 */
sealed trait IPResult {
    def isNoResult: Boolean
    def isFallThroughResult: Boolean

    def isFinal: Boolean
    final def isRefinable: Boolean = !isFinal

    def asFinal: FinalIPResult
    def asNonRefinable: NonRefinableIPResult
    def asRefinable: RefinableIPResult

    def e: (DefinedMethod, Int) = (method, pc)
    def method: DefinedMethod
    def pc: Int

    def sciOpt: Option[StringConstancyInformation]
}

object IPResult {
    def unapply(ipResult: IPResult): Some[(DefinedMethod, Int)] = Some((ipResult.method, ipResult.pc))
}

sealed trait NonRefinableIPResult extends IPResult {
    override final def isFinal: Boolean = true
    override def asNonRefinable: NonRefinableIPResult = this
    override def asRefinable: RefinableIPResult = throw new UnsupportedOperationException()
}

sealed trait RefinableIPResult extends IPResult {
    override final def isFinal: Boolean = false

    override final def asFinal: FinalIPResult = throw new UnsupportedOperationException()
    override def asNonRefinable: NonRefinableIPResult = throw new UnsupportedOperationException()
    override def asRefinable: RefinableIPResult = this
}

/**
 * Indicates that the pc is not relevant to the interpretation of the analyses.
 *
 * @note Since the pc is not relevant, we are able to return a final result with the neutral element when needed.
 *       Interpreters handling this result type should either convert it to
 *       [[StringConstancyInformation.getNeutralElement]] or preserve it.
 */
case class NoIPResult(method: DefinedMethod, pc: Int) extends NonRefinableIPResult {
    override def isNoResult: Boolean = true
    override def isFallThroughResult: Boolean = false

    override def asFinal: FinalIPResult = FinalIPResult(StringConstancyInformation.getNeutralElement, method, pc)
    override def sciOpt: Option[StringConstancyInformation] = None
}

sealed trait SomeIPResult extends IPResult {
    override final def isNoResult: Boolean = false
    override final def isFallThroughResult: Boolean = false
}

case class EmptyIPResult(method: DefinedMethod, pc: Int) extends RefinableIPResult with SomeIPResult {
    override def sciOpt: Option[StringConstancyInformation] = None
}

sealed trait ValueIPResult extends SomeIPResult {
    override final def sciOpt: Option[StringConstancyInformation] = Some(sci)

    def sci: StringConstancyInformation
}

object ValueIPResult {
    def unapply(valueIPResult: ValueIPResult): Some[StringConstancyInformation] = Some(valueIPResult.sci)
}

case class FinalIPResult(
    override val sci: StringConstancyInformation,
    method:           DefinedMethod,
    pc:               Int
) extends NonRefinableIPResult with ValueIPResult {
    override def asFinal: FinalIPResult = this
}

object FinalIPResult {
    def nullElement(method: DefinedMethod, pc: Int) =
        new FinalIPResult(StringConstancyInformation.getNullElement, method, pc)
    def lb(method: DefinedMethod, pc: Int) = new FinalIPResult(StringConstancyInformation.lb, method, pc)
}

case class InterimIPResult private (
    override val sci:     StringConstancyInformation,
    override val method:  DefinedMethod,
    override val pc:      Int,
    ipResultDependees:    Iterable[RefinableIPResult]  = List.empty,
    ipResultContinuation: Option[IPResult => IPResult] = None,
    epsDependees:         Iterable[SomeEOptionP]       = List.empty,
    epsContinuation:      Option[SomeEPS => IPResult]  = None
) extends RefinableIPResult with ValueIPResult

object InterimIPResult {
    def fromRefinableIPResults(
        sci:                  StringConstancyInformation,
        method:               DefinedMethod,
        pc:                   Int,
        refinableResults:     Iterable[RefinableIPResult],
        ipResultContinuation: IPResult => IPResult
    ) = new InterimIPResult(sci, method, pc, refinableResults, Some(ipResultContinuation))

    def lbWithIPResultDependees(
        method:               DefinedMethod,
        pc:                   Int,
        refinableResults:     Iterable[RefinableIPResult],
        ipResultContinuation: IPResult => IPResult
    ) = new InterimIPResult(StringConstancyInformation.lb, method, pc, refinableResults, Some(ipResultContinuation))

    def fromRefinableEPSResults(
        sci:             StringConstancyInformation,
        method:          DefinedMethod,
        pc:              Int,
        epsDependees:    Iterable[SomeEOptionP],
        epsContinuation: SomeEPS => IPResult
    ) = new InterimIPResult(
        sci,
        method,
        pc,
        epsDependees = epsDependees,
        epsContinuation = Some(epsContinuation)
    )

    def lbWithEPSDependees(
        method:          DefinedMethod,
        pc:              Int,
        epsDependees:    Iterable[SomeEOptionP],
        epsContinuation: SomeEPS => IPResult
    ) = new InterimIPResult(
        StringConstancyInformation.lb,
        method,
        pc,
        epsDependees = epsDependees,
        epsContinuation = Some(epsContinuation)
    )

    def unapply(interimIPResult: InterimIPResult): Some[(
        StringConstancyInformation,
        DefinedMethod,
        Int,
        Iterable[_],
        (_) => IPResult
    )] = {
        if (interimIPResult.ipResultContinuation.isDefined) {
            Some((
                interimIPResult.sci,
                interimIPResult.method,
                interimIPResult.pc,
                interimIPResult.ipResultDependees,
                interimIPResult.ipResultContinuation.get
            ))
        } else {
            Some((
                interimIPResult.sci,
                interimIPResult.method,
                interimIPResult.pc,
                interimIPResult.epsDependees,
                interimIPResult.epsContinuation.get
            ))
        }
    }
}
