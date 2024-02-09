/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @author Maximilian Rüsch
 */
trait StringInterpreter[State <: ComputationState[State]] {

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
        s:  State
    ): (EOptionP[Method, TACAI], Option[TAC]) = {
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
     * Extracts all parameters of the function calls at the given `pcs`.
     */
    protected def getParametersForPCs(pcs: Iterable[Int])(implicit state: State): List[Seq[Expr[V]]] = {
        val paramLists = ListBuffer[Seq[Expr[V]]]()
        pcs.map(state.tac.pcToIndex).foreach { stmtIndex =>
            val params = state.tac.stmts(stmtIndex) match {
                case ExprStmt(_, vfc: FunctionCall[V])     => vfc.params
                case Assignment(_, _, fc: FunctionCall[V]) => fc.params
                case _                                     => Seq()
            }
            if (params.nonEmpty) {
                paramLists.append(params)
            }
        }
        paramLists.toList
    }

    /**
     * evaluateParameters takes a list of parameters, `params`, as produced, e.g., by
     * [[StringInterpreter.getParametersForPCs]], and an interpretation handler, `iHandler`
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
        params:   List[Seq[Expr[V]]],
        iHandler: InterpretationHandler[State],
        funCall:  FunctionCall[V]
    )(implicit state: State): NonFinalFunctionArgs = ListBuffer.from(params.zipWithIndex.map {
        case (nextParamList, outerIndex) =>
            ListBuffer.from(nextParamList.zipWithIndex.map {
                case (nextParam, middleIndex) =>
                    ListBuffer.from(nextParam.asVar.definedBy.toArray.sorted.zipWithIndex.map {
                        case (ds, innerIndex) =>
                            val ep = iHandler.processDefSite(ds)
                            if (ep.isRefinable) {
                                if (!state.nonFinalFunctionArgsPos.contains(funCall)) {
                                    state.nonFinalFunctionArgsPos(funCall) = mutable.Map()
                                }
                                val e = ep.e.asInstanceOf[SContext]
                                state.nonFinalFunctionArgsPos(funCall)(e) = (outerIndex, middleIndex, innerIndex)
                                if (!state.entity2Function.contains(e)) {
                                    state.entity2Function(e) = ListBuffer()
                                }
                                state.entity2Function(e).append(funCall)
                            }
                            ep
                    })
            })
    })

    /**
     * Checks whether the interpretation of parameters, as, e.g., produced by [[evaluateParameters()]], is final or not
     * and returns all refinable results as a list. Hence, an empty list is returned, all parameters are fully evaluated.
     */
    protected def getRefinableParameterResults(
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
        evaluatedParameters: Seq[Seq[Seq[FinalEP[Entity, StringConstancyProperty]]]]
    ): ListBuffer[ListBuffer[StringConstancyInformation]] =
        ListBuffer.from(evaluatedParameters.map { paramList =>
            ListBuffer.from(paramList.map { param =>
                StringConstancyInformation.reduceMultiple(param.map { _.p.stringConstancyInformation })
            })
        })
}
