/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ObjectType
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[VirtualFunctionCall]]s in an interprocedural fashion.
 * The list of currently supported function calls can be seen in the documentation of [[interpret]].
 *
 * @author Patrick Mell
 */
class L1VirtualFunctionCallInterpreter(
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: InterpretationHandler[L1ComputationState],
        ps:                                 PropertyStore,
        params:                             List[Seq[StringConstancyInformation]],
        contextProvider:                    ContextProvider
) extends L1StringInterpreter[L1ComputationState] {

    override type T = VirtualFunctionCall[V]

    /**
     * Currently, this implementation supports the interpretation of the following function calls:
     * <ul>
     * <li>`append`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]].</li>
     * <li>
     *     `toString`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]]. As
     *     a `toString` call does not change the state of such an object, an empty list will be
     *     returned.
     * </li>
     * <li>
     *     `replace`: Calls to the `replace` function of [[StringBuilder]] and [[StringBuffer]]. For
     *     further information how this operation is processed, see
     *     [[L1VirtualFunctionCallInterpreter.interpretReplaceCall]].
     * </li>
     * <li>
     *     Apart from these supported methods, a list with [[StringConstancyProperty.lb]]
     *     will be returned in case the passed method returns a [[java.lang.String]].
     * </li>
     * </ul>
     *
     * If none of the above-described cases match, a final result containing
     * [[StringConstancyProperty.lb]] is returned.
     *
     * @note This function takes care of updating [[state.fpe2sci]] as necessary.
     */
    override def interpret(instr: T, defSite: Int)(implicit
        state: L1ComputationState
    ): EOptionP[Entity, StringConstancyProperty] = {
        val result = instr.name match {
            case "append"   => interpretAppendCall(instr)
            case "toString" => interpretToStringCall(instr)
            case "replace"  => Some(interpretReplaceCall)
            case _ =>
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj == ObjectType.String =>
                        interpretArbitraryCall(instr, defSite)
                    case _ =>
                        Some(StringConstancyInformation.lb)
                }
        }

        if (result.isDefined) {
            state.appendToFpe2Sci(defSite, result.get)
        }
        FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty(result.getOrElse(StringConstancyInformation.lb)))
    }

    /**
     * This function interprets an arbitrary [[VirtualFunctionCall]]. If this method returns a
     * [[FinalEP]] instance, the interpretation of this call is already done. Otherwise, a new
     * analysis was triggered whose result is not yet ready. In this case, the result needs to be
     * finalized later on.
     */
    private def interpretArbitraryCall(instr: T, defSite: Int)(
        implicit state: L1ComputationState
    ): Option[StringConstancyInformation] = {
        val (methods, _) = getMethodsForPC(instr.pc)(ps, state.callees, contextProvider)

        if (methods.isEmpty) {
            return Some(StringConstancyInformation.lb)
        }
        // TODO: Type Iterator!
        val directCallSites = state.callees.directCallSites(NoContext)(ps, contextProvider)
        val instrClassName = instr.receiver.asVar.value.asReferenceValue.asReferenceType.mostPreciseObjectType.toJava

        val relevantPCs = directCallSites.filter {
            case (_, calledMethods) => calledMethods.exists { m =>
                    val mClassName = m.method.declaringClassType.toJava
                    m.method.name == instr.name && mClassName == instrClassName
                }
        }.keys

        // Collect all parameters; either from the state, if the interpretation of instr was started
        // before (in this case, the assumption is that all parameters are fully interpreted) or
        // start a new interpretation
        val params = if (state.nonFinalFunctionArgs.contains(instr)) {
            state.nonFinalFunctionArgs(instr)
        } else {
            evaluateParameters(
                getParametersForPCs(relevantPCs, state.tac),
                exprHandler,
                instr,
                state.nonFinalFunctionArgsPos,
                state.entity2Function
            )
        }
        // Continue only when all parameter information are available
        val nonFinalResults = getNonFinalParameters(params.toSeq.map(t => t.toSeq.map(_.toSeq)))
        if (nonFinalResults.nonEmpty) {
            state.nonFinalFunctionArgs(instr) = params
            return None
        }

        state.nonFinalFunctionArgs.remove(instr)
        state.nonFinalFunctionArgsPos.remove(instr)
        val evaluatedParams = convertEvaluatedParameters(params.toSeq.map(t => t.toSeq.map(_.toSeq.map(_.asFinal))))
        val results = methods.map { nextMethod =>
            val (_, tac) = getTACAI(ps, nextMethod, state)
            if (tac.isDefined) {
                state.methodPrep2defSite.remove(nextMethod)
                val returns = tac.get.stmts.filter(_.isInstanceOf[ReturnValue[V]])
                if (returns.isEmpty) {
                    // It might be that a function has no return value, e. g., in case it is
                    // guaranteed to throw an exception
                    FinalEP(instr, StringConstancyProperty.lb)
                } else {
                    val results = returns.map { ret =>
                        val entity =
                            (ret.asInstanceOf[ReturnValue[V]].expr.asVar.toPersistentForm(tac.get.stmts), nextMethod)
                        L1StringAnalysis.registerParams(entity, evaluatedParams)
                        ps(entity, StringConstancyProperty.key) match {
                            case r: FinalEP[SContext, StringConstancyProperty] =>
                                state.appendToFpe2Sci(defSite, r.p.stringConstancyInformation)
                                r
                            case eps =>
                                state.dependees = eps :: state.dependees
                                state.appendToVar2IndexMapping(entity._1, defSite)
                                eps
                        }
                    }
                    results.find(_.isRefinable).getOrElse(results.head)
                }
            } else {
                state.appendToMethodPrep2defSite(nextMethod, defSite)
                EPK(state.entity, StringConstancyProperty.key)
            }
        }

        val finalResults = results.filter(_.isFinal)
        // val intermediateResults = results.filter(_.isRefinable)
        if (results.length == finalResults.length) {
            Some(finalResults.head.asFinal.p.stringConstancyInformation)
        } else {
            None
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(appendCall: VirtualFunctionCall[V])(
        implicit state: L1ComputationState
    ): Option[StringConstancyInformation] = {
        val receiverResults = receiverValuesOfAppendCall(appendCall)
        val appendResult = valueOfAppendCall(appendCall)

        // If there is an intermediate result, return this one (then the final result cannot yet be computed)
        if (receiverResults.head.isRefinable) {
            return None
        } else if (appendResult.isRefinable) {
            return None
        }

        val receiverScis = receiverResults.map {
            _.asFinal.p.stringConstancyInformation
        }
        val appendSci = appendResult.asFinal.p.stringConstancyInformation

        // The case can occur that receiver and append value are empty; although, it is
        // counter-intuitive, this case may occur if both, the receiver and the parameter, have been
        // processed before
        val areAllReceiversNeutral = receiverScis.forall(_.isTheNeutralElement)
        val finalSci = if (areAllReceiversNeutral && appendSci.isTheNeutralElement) {
            StringConstancyInformation.getNeutralElement
        } // It might be that we have to go back as much as to a New expression. As they do not
        // produce a result (= empty list), the if part
        else if (areAllReceiversNeutral) {
            appendSci
        } // The append value might be empty, if the site has already been processed (then this
        // information will come from another StringConstancyInformation object
        else if (appendSci.isTheNeutralElement) {
            StringConstancyInformation.reduceMultiple(receiverScis)
        } // Receiver and parameter information are available => Combine them
        else {
            val receiverSci = StringConstancyInformation.reduceMultiple(receiverScis)
            StringConstancyInformation(
                StringConstancyLevel.determineForConcat(
                    receiverSci.constancyLevel,
                    appendSci.constancyLevel
                ),
                StringConstancyType.APPEND,
                receiverSci.possibleStrings + appendSci.possibleStrings
            )
        }

        Some(finalSci)
    }

    /**
     * This function determines the current value of the receiver object of an `append` call. For
     * the result list, there is the following convention: A list with one element of type
     * [[org.opalj.fpcf.InterimResult]] indicates that a final result for the receiver value could
     * not be computed. Otherwise, the result list will contain >= 1 elements of type [[FinalEP]]
     * indicating that all final results for the receiver value are available.
     *
     * @note All final results computed by this function are put int [[state.fpe2sci]] even if the
     *       returned list contains an [[org.opalj.fpcf.InterimResult]].
     */
    private def receiverValuesOfAppendCall(
        call: VirtualFunctionCall[V]
    )(implicit state: L1ComputationState): List[EOptionP[Entity, StringConstancyProperty]] = {
        val defSites = call.receiver.asVar.definedBy.toArray.sorted

        val allResults = defSites.map(ds => (ds, exprHandler.processDefSite(ds, params)))
        val finalResults = allResults.filter(_._2.isFinal)
        val finalResultsWithoutNeutralElements = finalResults.filter {
            case (_, FinalEP(_, p: StringConstancyProperty)) =>
                !p.stringConstancyInformation.isTheNeutralElement
            case _ => false
        }
        val intermediateResults = allResults.filter(_._2.isRefinable)

        // Extend the state by the final results not being the neutral elements (they might need to be finalized later)
        finalResultsWithoutNeutralElements.foreach { next =>
            state.appendToFpe2Sci(next._1, next._2.asFinal.p.stringConstancyInformation)
        }

        if (intermediateResults.isEmpty) {
            finalResults.map(_._2).toList
        } else {
            List(intermediateResults.head._2)
        }
    }

    /**
     * Determines the (string) value that was passed to a `String{Builder, Buffer}#append` method.
     * This function can process string constants as well as function calls as argument to append.
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V]
    )(implicit state: L1ComputationState): EOptionP[Entity, StringConstancyProperty] = {
        // .head because we want to evaluate only the first argument of append
        val param = call.params.head.asVar
        val defSites = param.definedBy.toArray.sorted
        val values = defSites.map(exprHandler.processDefSite(_, params))

        // Defer the computation if there is at least one intermediate result
        if (values.exists(_.isRefinable)) {
            return values.find(_.isRefinable).get
        }

        val sciValues = values.map { _.asFinal.p.stringConstancyInformation }
        val defSitesValueSci = StringConstancyInformation.reduceMultiple(sciValues)
        // If defSiteHead points to a "New", value will be the empty list. In that case, process
        // the first use site
        var newValueSci = StringConstancyInformation.getNeutralElement
        if (defSitesValueSci.isTheNeutralElement) {
            val headSite = defSites.head
            if (headSite < 0) {
                newValueSci = StringConstancyInformation.lb
            } else {
                val ds = cfg.code.instructions(headSite).asAssignment.targetVar.usedBy.toArray.min
                val r = exprHandler.processDefSite(ds, params)
                r match {
                    case FinalP(p) => newValueSci = p.stringConstancyInformation
                    // Defer the computation if there is no final result yet
                    case _ => return r
                }
            }
        } else {
            newValueSci = defSitesValueSci
        }

        val finalSci = param.value.computationalType match {
            // For some types, we know the (dynamic) values
            case ComputationalTypeInt =>
                // The value was already computed above; however, we need to check whether the
                // append takes an int value or a char (if it is a constant char, convert it)
                if (call.descriptor.parameterType(0).isCharType &&
                    defSitesValueSci.constancyLevel == StringConstancyLevel.CONSTANT
                ) {
                    if (defSitesValueSci.isTheNeutralElement) {
                        StringConstancyProperty.lb.stringConstancyInformation
                    } else {
                        val charSciValues = sciValues.filter(_.possibleStrings != "") map { sci =>
                            if (isIntegerValue(sci.possibleStrings)) {
                                sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                            } else {
                                sci
                            }
                        }
                        StringConstancyInformation.reduceMultiple(charSciValues)
                    }
                } else {
                    newValueSci
                }
            case ComputationalTypeFloat =>
                InterpretationHandler.getConstancyInfoForDynamicFloat
            // Otherwise, try to compute
            case _ =>
                newValueSci
        }

        val e: Integer = defSites.head
        state.appendToFpe2Sci(e, newValueSci, reset = true)
        FinalEP(e, StringConstancyProperty(finalSci))
    }

    /**
     * Function for processing calls to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     * Note that this function assumes that the given `toString` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(call: VirtualFunctionCall[V])(
        implicit state: L1ComputationState
    ): Option[StringConstancyInformation] =
        handleInterpretationResult(exprHandler.processDefSite(call.receiver.asVar.definedBy.head, params))

    /**
     * Function for processing calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     * (Currently, this function simply approximates `replace` functions by returning the lower
     * bound of [[StringConstancyProperty]]).
     */
    private def interpretReplaceCall: StringConstancyInformation =
        InterpretationHandler.getStringConstancyInformationForReplace

    /**
     * Checks whether a given string is an integer value, i.e. contains only numbers.
     */
    private def isIntegerValue(toTest: String): Boolean = toTest.forall(_.isDigit)
}
