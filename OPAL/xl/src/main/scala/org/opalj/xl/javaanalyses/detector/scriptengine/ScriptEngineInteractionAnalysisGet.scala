/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.xl.translator.JavaJavaScriptTranslator
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.Coordinator
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.Coordinator.V

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.tac.fpcf.analyses.cg.AllocationsUtil
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.Expr
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisState
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.TACAIBasedAPIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil

abstract class ScriptEngineInteractionAnalysisGet(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def java2js = JavaJavaScriptTranslator.Java2JavaScript[PointsToSet, ContextType]

    def js2java = JavaJavaScriptTranslator.JavaScript2Java[PointsToSet, ContextType]

    def handleJSResult(possibleStrings: Set[String], store: Map[Any, Any], targetVarDefSite: Entity)(
        implicit
        pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    ): Unit = {

        val possibleValues = possibleStrings.map(variableName => store.asInstanceOf[Map[PKey, Value]]
            .getOrElse(PKey.StringPKey.make(variableName), Value.makeUndef()))

        val (referenceTypes, pointsToSetSet, index) = js2java(possibleValues)

        pointsToSetSet.foreach { pointsToAnalysisState.includeSharedPointsToSet(targetVarDefSite, _) }

        val jsPointsToSet = this.createPointsToSet(index, NoContext.asInstanceOf[ContextType], referenceTypes.head, false, false)
        pointsToAnalysisState.includeSharedPointsToSet(targetVarDefSite, jsPointsToSet)
    }

    def c(
        receiverOption:   Option[Expr[V]],
        callIndex:        Int,
        possibleStrings:  Set[String],
        targetVarDefSite: Entity,
        context:          ContextType,
        param:            V,
        stmts:            Array[Stmt[V]],
        oldDependees:     Set[SomeEOptionP]
    )(eps: SomeEPS)(implicit
        typeIteratorState: TypeIteratorState,
                    pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    ): ProperPropertyComputationResult = {
        var dependees: Set[SomeEOptionP] = oldDependees.filter(x => x.e != eps.e)
        val epk = eps.toEPK
        eps match {

            case InterimUBP(InterimAnalysisResult(store)) =>
                handleJSResult(possibleStrings, store, targetVarDefSite)
                dependees += eps

            case UBP(newPointsTo: PointsToSet @unchecked) =>
                newPointsTo.forNewestNElements(newPointsTo.numElements) { alloc =>
                    {
                        val instance = Coordinator.ScriptEngineInstance(alloc)

                        propertyStore(instance, AnalysisResult.key) match {

                            case ubp @ InterimUBP(InterimAnalysisResult(store)) =>
                                handleJSResult(possibleStrings, store, targetVarDefSite)
                                dependees += ubp

                            case eps => dependees += eps
                        }
                    }
                }
                dependees += eps

            case eps => dependees += eps
        }

        if (typeIteratorState.hasDependee(epk)) {
            AllocationsUtil.continuationForAllocation[None.type, ContextType](
                eps, context, _ => (param, stmts), _ => true, _ => {
                throw new Exception("TODO: What to do if param is unknown?")
            }
            ) { (_, _, allocationIndex, stmts) =>
                val newParam = StringUtil.getString(allocationIndex, stmts)
                if (newParam.isEmpty)
                    throw new Exception("TODO: What to do if param is unknown?")

                receiverOption.get.asVar.definedBy.foreach(defSite => {
                    val allocations = currentPointsToOfDefSite("getTarget", defSite)
                    allocations.forNewestNElements(allocations.numElements) { alloc =>
                        {
                            val instance = Coordinator.ScriptEngineInstance(alloc)
                            propertyStore(instance, AnalysisResult.key) match {
                                case ubp @ InterimUBP(InterimAnalysisResult(store)) =>
                                    handleJSResult(possibleStrings, store, targetVarDefSite)
                                    dependees += ubp
                                case eps => dependees += eps
                            }
                        }
                    }
                })
            }

            typeIteratorState.updateDependency(eps)
        }

        dependees = typeIteratorState.dependees ++ dependees

        Results(createResults, InterimPartialResult(
            List.empty[SomePartialResult],
            dependees,
            c(
                receiverOption,
                callIndex,
                possibleStrings,
                targetVarDefSite,
                context,
                param,
                stmts,
                dependees
            )
        ))
    }

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        callPC:          Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        println("start get analysis")
        implicit val pointsToAnalysisState: PointsToAnalysisState[ElementType, PointsToSet, ContextType] =
            new PointsToAnalysisState(callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac)))

        implicit val typeIteratorState: TypeIteratorState = new BaseAnalysisState with TypeIteratorState

        if (params.head.isEmpty)
            return Results(); // Cannot determine any result here

        val param = params.head.get.asVar
        val possibleStrings =
            StringUtil.getPossibleStrings(param, callerContext, None, tac.stmts, () => {
                throw new Exception("TODO: What to do if param is unknown?")
            })

        var dependees: Set[SomeEOptionP] = Set.empty

        val targetVarDefSite = getDefSite(callPC)
        receiverOption.get.asVar.definedBy.foreach(defSite => {
            val allocations = currentPointsToOfDefSite("getTarget", defSite)
            allocations.forNewestNElements(allocations.numElements) { alloc =>
                {
                    val instance = Coordinator.ScriptEngineInstance(alloc)
                    val result = propertyStore(instance, AnalysisResult.key)
                    println(s"get analysis querying result: $result")
                    result match {
                        case InterimUBP(InterimAnalysisResult(store)) =>
                            val possibleValues =
                                possibleStrings.map(variableName => store.asInstanceOf[Map[PKey, Value]]
                                    .getOrElse(PKey.StringPKey.make(variableName), Value.makeUndef()))

                            val pointsToSetSet = js2java(possibleValues)._2

                            pointsToSetSet.foreach { pointsToSet =>
                                pointsToAnalysisState.includeSharedPointsToSet(
                                    targetVarDefSite, pointsToSet
                                )
                            }

                        case eps => dependees += eps
                    }
                }
            }
            val getTargetDependees = pointsToAnalysisState.dependeesOf("getTarget")
            dependees ++= getTargetDependees.valuesIterator.map(_._1)
        })

        Results(createResults, InterimPartialResult(List.empty[SomePartialResult], dependees, c(
            receiverOption,
            callPC, possibleStrings, targetVarDefSite,
            callerContext, param, tac.stmts, dependees
        )))
    }
}
