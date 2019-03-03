/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.ReturnValue
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralStringAnalysis

/**
 * The `InterproceduralVirtualFunctionCallInterpreter` is responsible for processing
 * [[VirtualFunctionCall]]s in an interprocedural fashion.
 * The list of currently supported function calls can be seen in the documentation of [[interpret]].
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class VirtualFunctionCallPreparationInterpreter(
        cfg:             CFG[Stmt[V], TACStmts[V]],
        exprHandler:     InterproceduralInterpretationHandler,
        ps:              PropertyStore,
        state:           InterproceduralComputationState,
        declaredMethods: DeclaredMethods,
        params:          List[Seq[StringConstancyInformation]],
        c:               ProperOnUpdateContinuation
) extends AbstractStringInterpreter(cfg, exprHandler) {

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
     *     [[VirtualFunctionCallPreparationInterpreter.interpretReplaceCall]].
     * </li>
     * <li>
     *     Apart from these supported methods, a list with [[StringConstancyProperty.lb]]
     *     will be returned in case the passed method returns a [[java.lang.String]].
     * </li>
     * </ul>
     *
     * If none of the above-described cases match, a final result containing
     * [[StringConstancyProperty.getNeutralElement]] is returned.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @note This function takes care of updating [[state.fpe2sci]] as necessary.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val result = instr.name match {
            case "append"   ⇒ interpretAppendCall(instr, defSite)
            case "toString" ⇒ interpretToStringCall(instr)
            case "replace"  ⇒ interpretReplaceCall(instr)
            case _ ⇒
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj.fqn == "java/lang/String" ⇒
                        interpretArbitraryCall(instr, defSite)
                    case _ ⇒
                        val e: Integer = defSite
                        Result(e, StringConstancyProperty.getNeutralElement)
                }
        }

        result match {
            case r: Result ⇒ state.appendResultToFpe2Sci(defSite, r)
            case _         ⇒
        }
        result
    }

    /**
     * This function interprets an arbitrary [[VirtualFunctionCall]]. If this method returns a
     * [[Result]] instance, the interpretation of this call is already done. Otherwise, a new
     * analysis was triggered whose result is not yet ready. In this case, the result needs to be
     * finalized later on.
     */
    private def interpretArbitraryCall(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val (methods, _) = getMethodsForPC(
            instr.pc, ps, state.callees, declaredMethods
        )

        if (methods.isEmpty) {
            return Result(instr, StringConstancyProperty.lb)
        }

        val directCallSites = state.callees.directCallSites()(ps, declaredMethods)
        val instrClassName =
            instr.receiver.asVar.value.asReferenceValue.asReferenceType.mostPreciseObjectType.toJava
        val relevantPCs = directCallSites.filter {
            case (_, calledMethods) ⇒ calledMethods.exists { m ⇒
                val mClassName = m.declaringClassType.toJava
                m.name == instr.name && mClassName == instrClassName
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
        val nonFinalResults = getNonFinalParameters(params)
        if (nonFinalResults.nonEmpty) {
            state.nonFinalFunctionArgs(instr) = params
            return nonFinalResults.head
        }

        state.nonFinalFunctionArgs.remove(instr)
        state.nonFinalFunctionArgsPos.remove(instr)
        val evaluatedParams = convertEvaluatedParameters(params)
        val results = methods.map { nextMethod ⇒
            val tac = getTACAI(ps, nextMethod, state)
            if (tac.isDefined) {
                // It might be that a function has no return value, e. g., in case it is guaranteed
                // to throw an exception (see, e.g.,
                // com.sun.org.apache.xpath.internal.objects.XRTreeFragSelectWrapper#str)
                if (!tac.get.stmts.exists(_.isInstanceOf[ReturnValue[V]])) {
                    Result(instr, StringConstancyProperty.lb)
                } else {
                    // TAC and return available => Get return UVar and start the string analysis
                    val ret = tac.get.stmts.find(_.isInstanceOf[ReturnValue[V]]).get
                    val uvar = ret.asInstanceOf[ReturnValue[V]].expr.asVar
                    val entity = (uvar, nextMethod)

                    InterproceduralStringAnalysis.registerParams(entity, evaluatedParams)
                    val eps = ps(entity, StringConstancyProperty.key)
                    eps match {
                        case r: Result ⇒
                            state.appendResultToFpe2Sci(defSite, r)
                            r
                        case _ ⇒
                            state.dependees = eps :: state.dependees
                            state.var2IndexMapping(uvar) = defSite
                            InterimResult(
                                entity,
                                StringConstancyProperty.lb,
                                StringConstancyProperty.ub,
                                List(),
                                c
                            )
                    }
                }
            } else {
                // No TAC => Register dependee and continue
                InterimResult(
                    nextMethod,
                    StringConstancyProperty.lb,
                    StringConstancyProperty.ub,
                    state.dependees,
                    c
                )
            }
        }

        val finalResults = results.filter(_.isInstanceOf[Result])
        val intermediateResults = results.filter(!_.isInstanceOf[Result])
        if (results.length == finalResults.length) {
            finalResults.head
        } else {
            intermediateResults.head
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(
        appendCall: VirtualFunctionCall[V], defSite: Int
    ): ProperPropertyComputationResult = {
        val receiverResults = receiverValuesOfAppendCall(appendCall, state)
        val appendResult = valueOfAppendCall(appendCall, state)

        // If there is an intermediate result, return this one (then the final result cannot yet be
        // computed)
        if (!receiverResults.head.isInstanceOf[Result]) {
            return receiverResults.head
        } else if (!appendResult.isInstanceOf[Result]) {
            return appendResult
        }

        val receiverScis = receiverResults.map {
            StringConstancyProperty.extractFromPPCR(_).stringConstancyInformation
        }
        val appendSci =
            StringConstancyProperty.extractFromPPCR(appendResult).stringConstancyInformation

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
                    receiverSci.constancyLevel, appendSci.constancyLevel
                ),
                StringConstancyType.APPEND,
                receiverSci.possibleStrings + appendSci.possibleStrings
            )
        }

        val e: Integer = defSite
        Result(e, StringConstancyProperty(finalSci))
    }

    /**
     * This function determines the current value of the receiver object of an `append` call. For
     * the result list, there is the following convention: A list with one element of type
     * [[org.opalj.fpcf.InterimResult]] indicates that a final result for the receiver value could
     * not be computed. Otherwise, the result list will contain >= 1 elements of type [[Result]]
     * indicating that all final results for the receiver value are available.
     *
     * @note All final results computed by this function are put int [[state.fpe2sci]] even if the
     *       returned list contains an [[org.opalj.fpcf.InterimResult]].
     */
    private def receiverValuesOfAppendCall(
        call: VirtualFunctionCall[V], state: InterproceduralComputationState
    ): List[ProperPropertyComputationResult] = {
        val defSites = call.receiver.asVar.definedBy.toArray.sorted

        val allResults = defSites.map(ds ⇒ (ds, exprHandler.processDefSite(ds, params)))
        val finalResults = allResults.filter(_._2.isInstanceOf[Result])
        val intermediateResults = allResults.filter(!_._2.isInstanceOf[Result])

        // Extend the state by the final results
        finalResults.foreach { next ⇒
            state.appendResultToFpe2Sci(next._1, next._2.asInstanceOf[Result])
        }

        if (allResults.length == finalResults.length) {
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
        call: VirtualFunctionCall[V], state: InterproceduralComputationState
    ): ProperPropertyComputationResult = {
        // .head because we want to evaluate only the first argument of append
        val param = call.params.head.asVar
        val defSites = param.definedBy.toArray.sorted
        val values = defSites.map(exprHandler.processDefSite(_, params))

        // Defer the computation if there is at least one intermediate result
        if (!values.forall(_.isInstanceOf[Result])) {
            return values.find(!_.isInstanceOf[Result]).get
        }

        var valueSci = StringConstancyInformation.reduceMultiple(values.map {
            StringConstancyProperty.extractFromPPCR(_).stringConstancyInformation
        })
        // If defSiteHead points to a "New", value will be the empty list. In that case, process
        // the first use site (which is the <init> call)
        if (valueSci.isTheNeutralElement) {
            val ds = cfg.code.instructions(defSites.head).asAssignment.targetVar.usedBy.toArray.min
            val r = exprHandler.processDefSite(ds, params)
            // Again, defer the computation if there is no final result (yet)
            if (!r.isInstanceOf[Result]) {
                return r
            } else {
                valueSci = StringConstancyProperty.extractFromPPCR(r).stringConstancyInformation
            }
        }

        val finalSci = param.value.computationalType match {
            // For some types, we know the (dynamic) values
            case ComputationalTypeInt ⇒
                // The value was already computed above; however, we need to check whether the
                // append takes an int value or a char (if it is a constant char, convert it)
                if (call.descriptor.parameterType(0).isCharType &&
                    valueSci.constancyLevel == StringConstancyLevel.CONSTANT) {
                    valueSci.copy(
                        possibleStrings = valueSci.possibleStrings.toInt.toChar.toString
                    )
                } else {
                    valueSci
                }
            case ComputationalTypeFloat ⇒
                InterpretationHandler.getConstancyInfoForDynamicFloat
            // Otherwise, try to compute
            case _ ⇒
                valueSci
        }

        val e: Integer = defSites.head
        state.appendToFpe2Sci(e, valueSci, reset = true)
        Result(e, StringConstancyProperty(finalSci))
    }

    /**
     * Function for processing calls to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     * Note that this function assumes that the given `toString` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(
        call: VirtualFunctionCall[V]
    ): ProperPropertyComputationResult =
        // TODO: Can it produce an intermediate result???
        exprHandler.processDefSite(call.receiver.asVar.definedBy.head, params)

    /**
     * Function for processing calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     * (Currently, this function simply approximates `replace` functions by returning the lower
     * bound of [[StringConstancyProperty]]).
     */
    private def interpretReplaceCall(
        instr: VirtualFunctionCall[V]
    ): ProperPropertyComputationResult =
        Result(instr, InterpretationHandler.getStringConstancyPropertyForReplace)

}
