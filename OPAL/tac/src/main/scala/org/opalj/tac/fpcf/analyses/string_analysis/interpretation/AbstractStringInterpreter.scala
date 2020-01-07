/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.cfg.CFG
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.FunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.InterproceduralInterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.NonFinalFunctionArgs
import org.opalj.tac.fpcf.analyses.string_analysis.NonFinalFunctionArgsPos
import org.opalj.tac.fpcf.analyses.string_analysis.P

/**
 * @param cfg The control flow graph that underlies the instruction to interpret.
 * @param exprHandler In order to interpret an instruction, it might be necessary to interpret
 *                    another instruction in the first place. `exprHandler` makes this possible.
 *
 * @note The abstract type [[InterpretationHandler]] allows the handling of different styles (e.g.,
 *       intraprocedural and interprocedural). Thus, implementation of this class are required to
 *       clearly indicate what kind of [[InterpretationHandler]] they expect in order to ensure the
 *       desired behavior and not confuse developers.
 *
 * @author Patrick Mell
 */
abstract class AbstractStringInterpreter(
        protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        protected val exprHandler: InterpretationHandler
) {

    type T <: Any

    /**
     * Returns the EPS retrieved from querying the given property store for the given method as well
     * as the TAC, if it could already be determined. If not, thus function registers a dependee
     * within the given state.
     *
     * @param ps The property store to use.
     * @param m The method to get the TAC for.
     * @param s The computation state whose dependees might be extended in case the TAC is not
     *          immediately ready.
     * @return Returns (eps, tac).
     */
    protected def getTACAI(
        ps: PropertyStore,
        m:  Method,
        s:  InterproceduralComputationState
    ): (EOptionP[Method, TACAI], Option[TACode[TACMethodParameter, V]]) = {
        val tacai = ps(m, TACAI.key)
        if (tacai.hasUBP) {
            (tacai, tacai.ub.tac)
        } else {
            if (tacai.isRefinable) {
                s.dependees = tacai :: s.dependees
            }
            (tacai, None)
        }
    }

    /**
     * This function returns all methods for a given `pc` among a set of `declaredMethods`. The
     * second return value indicates whether at least one method has an unknown body (if `true`,
     * then there is such a method).
     */
    protected def getMethodsForPC(
        implicit
        pc: Int, ps: PropertyStore, callees: Callees, declaredMethods: DeclaredMethods
    ): (List[Method], Boolean) = {
        var hasMethodWithUnknownBody = false
        val methods = ListBuffer[Method]()
        callees.callees(pc).foreach {
            case definedMethod: DefinedMethod ⇒ methods.append(definedMethod.definedMethod)
            case _                            ⇒ hasMethodWithUnknownBody = true
        }

        (methods.sortBy(_.classFile.fqn).toList, hasMethodWithUnknownBody)
    }

    /**
     * `getParametersForPCs` takes a list of program counters, `pcs`, as well as the TACode on which
     * `pcs` is based. This function then extracts the parameters of all function calls from the
     * given `pcs` and returns them.
     */
    protected def getParametersForPCs(
        pcs: Iterable[Int],
        tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): List[Seq[Expr[V]]] = {
        val paramLists = ListBuffer[Seq[Expr[V]]]()
        pcs.map(tac.pcToIndex).foreach { stmtIndex ⇒
            val params = tac.stmts(stmtIndex) match {
                case ExprStmt(_, vfc: FunctionCall[V])     ⇒ vfc.params
                case Assignment(_, _, fc: FunctionCall[V]) ⇒ fc.params
                case _                                     ⇒ Seq()
            }
            if (params.nonEmpty) {
                paramLists.append(params)
            }
        }
        paramLists.toList
    }

    /**
     * evaluateParameters takes a list of parameters, `params`, as produced, e.g., by
     * [[AbstractStringInterpreter.getParametersForPCs]], and an interpretation handler, `iHandler`
     * and interprets the given parameters. The result list has the following format: The outer list
     * corresponds to the lists of parameters passed to a function / method, the list in the middle
     * corresponds to such lists and the inner-most list corresponds to the results /
     * interpretations (this list is required as a concrete parameter may have more than one
     * definition site).
     * For housekeeping, this function takes the function call, `funCall`, of which parameters are
     * to be evaluated as well as function argument positions, `functionArgsPos`, and a mapping from
     * entities to functions, `entity2function`.
     */
    protected def evaluateParameters(
        params:          List[Seq[Expr[V]]],
        iHandler:        InterproceduralInterpretationHandler,
        funCall:         FunctionCall[V],
        functionArgsPos: NonFinalFunctionArgsPos,
        entity2function: mutable.Map[P, ListBuffer[FunctionCall[V]]]
    ): NonFinalFunctionArgs = params.zipWithIndex.map {
        case (nextParamList, outerIndex) ⇒
            nextParamList.zipWithIndex.map {
                case (nextParam, middleIndex) ⇒
                    nextParam.asVar.definedBy.toArray.sorted.zipWithIndex.map {
                        case (ds, innerIndex) ⇒
                            val ep = iHandler.processDefSite(ds)
                            if (ep.isRefinable) {
                                if (!functionArgsPos.contains(funCall)) {
                                    functionArgsPos(funCall) = mutable.Map()
                                }
                                val e = ep.e.asInstanceOf[P]
                                functionArgsPos(funCall)(e) = (outerIndex, middleIndex, innerIndex)
                                if (!entity2function.contains(e)) {
                                    entity2function(e) = ListBuffer()
                                }
                                entity2function(e).append(funCall)
                            }
                            ep
                    }.to[ListBuffer]
            }.to[ListBuffer]
    }.to[ListBuffer]

    /**
     * This function checks whether the interpretation of parameters, as, e.g., produced by
     * [[evaluateParameters()]], is final or not and returns all refineables as a list. Hence, if
     * this function returns an empty list, all parameters are fully evaluated.
     */
    protected def getNonFinalParameters(
        evaluatedParameters: Seq[Seq[Seq[EOptionP[Entity, StringConstancyProperty]]]]
    ): List[EOptionP[Entity, StringConstancyProperty]] =
        evaluatedParameters.flatten.flatten.filter { _.isRefinable }.toList

    /**
     * convertEvaluatedParameters takes a list of evaluated / interpreted parameters as, e.g.,
     * produced by [[evaluateParameters]] and transforms these into a list of lists where the inner
     * lists are the reduced [[StringConstancyInformation]]. Note that this function assumes that
     * all results in the inner-most sequence are final!
     */
    protected def convertEvaluatedParameters(
        evaluatedParameters: Seq[Seq[Seq[EOptionP[Entity, Property]]]]
    ): ListBuffer[ListBuffer[StringConstancyInformation]] = evaluatedParameters.map { paramList ⇒
        paramList.map { param ⇒
            StringConstancyInformation.reduceMultiple(
                param.map {
                    _.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                }
            )
        }.to[ListBuffer]
    }.to[ListBuffer]

    /**
     *
     * @param instr The instruction that is to be interpreted. It is the responsibility of
     *              implementations to make sure that an instruction is properly and comprehensively
     *              evaluated.
     * @param defSite The definition site that corresponds to the given instruction. `defSite` is
     *                not necessary for processing `instr`, however, may be used, e.g., for
     *                housekeeping purposes. Thus, concrete implementations should indicate whether
     *                this value is of importance for (further) processing.
     * @return The interpreted instruction. A neutral StringConstancyProperty contained in the
     *         result indicates that an instruction was not / could not be interpreted (e.g.,
     *         because it is not supported or it was processed before).
     *         <p>
     *         As demanded by [[InterpretationHandler]], the entity of the result should be the
     *         definition site. However, as interpreters know the instruction to interpret but not
     *         the definition site, this function returns the interpreted instruction as entity.
     *         Thus, the entity needs to be replaced by the calling client.
     */
    def interpret(instr: T, defSite: Int): EOptionP[Entity, Property]

}
