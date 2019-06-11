/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
import org.opalj.value.IsReferenceValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.valueOriginsOfPCs
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI

/**
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToAnalysis[PointsToSet <: PointsToSetLike[_, _, PointsToSet]]
    extends AbstractPointsToBasedAnalysis[Entity, PointsToSet]
    with ReachableMethodAnalysis {

    var count = 0
    var usefull = 0

    def createPointsToSet(
        pc: Int, declaredMethod: DeclaredMethod, allocatedType: ObjectType
    ): PointsToSet

    override type State = PointsToAnalysisState[PointsToSet]

    override def processMethod(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        doProcessMethod(new PointsToAnalysisState[PointsToSet](definedMethod, tacEP))
    }

    // maps the points-to set of actual parameters (including *this*) the the formal parameters
    private[this] def handleCall(
        call: Call[DUVar[ValueInformation]], pc: Int
    )(implicit state: State): Unit = {
        val tac = state.tac
        val callees = state.callees(ps)
        val receiverOpt = call.receiverOption
        for (target ← callees.directCallees(pc)) {
            val fps = formalParameters(target)

            if (fps != null) {
                // handle receiver for non static methods
                if (receiverOpt.isDefined) {
                    val fp = fps(0)
                    // IMPROVE: Here we copy all points-to entries of the receiver into the *this*
                    // of the target methods. It would be only needed to do so for the ones that led
                    // to the call.
                    state.setOrUpdatePointsToSet(
                        fp, currentPointsTo(fp, receiverOpt.get.asVar.definedBy)
                    )
                }

                // in case of signature polymorphic methods, we give up
                if (call.params.size == target.descriptor.parametersCount) {
                    // handle params
                    for (i ← 0 until target.descriptor.parametersCount) {
                        val fp = fps(i + 1)
                        state.setOrUpdatePointsToSet(
                            fp, currentPointsTo(fp, call.params(i).asVar.definedBy)
                        )
                    }
                } else {
                    // todo: it should not be needed to mark it as incomplete
                }
            } else {
                state.addIncompletePointsToInfo(pc)
            }
        }

        // todo: reduce code duplication
        for (target ← callees.indirectCallees(pc)) {
            val fps = formalParameters(target)

            if (fps != null) {

                // handle receiver for non static methods
                val receiverOpt = callees.indirectCallReceiver(pc, target)
                if (receiverOpt.isDefined) {
                    val fp = fps(0)
                    state.setOrUpdatePointsToSet(
                        fp,
                        currentPointsTo(
                            fp, valueOriginsOfPCs(receiverOpt.get._2, tac.pcToIndex)
                        )
                    )
                } else {
                    // todo: distinguish between static methods and unavailable info
                }

                val indirectParams = callees.indirectCallParameters(pc, target)
                for (i ← 0 until target.descriptor.parametersCount) {
                    val fp = fps(i + 1)
                    val indirectParam = indirectParams(i)
                    if (indirectParam.isDefined) {
                        state.setOrUpdatePointsToSet(
                            fp,
                            currentPointsTo(
                                fp, valueOriginsOfPCs(indirectParam.get._2, tac.pcToIndex)
                            )
                        )
                    } else {
                        state.addIncompletePointsToInfo(pc)
                    }
                }
            } else {
                state.addIncompletePointsToInfo(pc)
            }
        }
    }

    private[this] def doProcessMethod(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        val tac = state.tac
        val method = state.method.definedMethod

        for (stmt ← tac.stmts) stmt match {
            case Assignment(pc, _, New(_, t)) ⇒
                val defSite = definitionSites(method, pc)
                state.setOrUpdatePointsToSet(defSite, createPointsToSet(pc, state.method, t))

            case Assignment(pc, _, NewArray(_, _, tpe)) if tpe.elementType.isObjectType ⇒
                val defSite = definitionSites(method, pc)
                state.setOrUpdatePointsToSet(
                    defSite,
                    createPointsToSet(pc, state.method, tpe.elementType.asObjectType)
                )

            // that case should not happen
            case Assignment(pc, DVar(_: IsReferenceValue, _), UVar(_, defSites)) ⇒
                val defSiteObject = definitionSites(method, pc)
                state.setOrUpdatePointsToSet(
                    defSiteObject, currentPointsTo(defSiteObject, defSites)
                )

            case Assignment(pc, DVar(_: IsReferenceValue, _), GetField(_, declaringClass, name, fieldType, _)) ⇒
                val defSiteObject = definitionSites(method, pc)
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    state.setOrUpdatePointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, fieldOpt.get)
                    )
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, DVar(_: IsReferenceValue, _), GetStatic(_, declaringClass, name, fieldType)) ⇒
                val defSiteObject = definitionSites(method, pc)
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    state.setOrUpdatePointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, fieldOpt.get)
                    )
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case Assignment(pc, _, ArrayLoad(_, _, UVar(av: IsSArrayValue, _))) if av.theUpperTypeBound.elementType.isObjectType ⇒
                val defSiteObject = definitionSites(method, pc)
                val arrayBaseType = av.theUpperTypeBound.elementType
                state.setOrUpdatePointsToSet(
                    defSiteObject,
                    currentPointsTo(defSiteObject, arrayBaseType)
                )

            // TODO: Use handleCall here
            case Assignment(pc, targetVar, call: FunctionCall[DUVar[ValueInformation]]) ⇒
                val targets = state.callees(ps).callees(pc)
                val defSiteObject = definitionSites(method, pc)

                if (targetVar.value.isReferenceValue) {
                    state.setOrUpdatePointsToSet(
                        defSiteObject,
                        targets.map(currentPointsTo(defSiteObject, _))
                    )
                }

                handleCall(call, pc)

            case Assignment(pc, targetVar, const: Const) if targetVar.value.isReferenceValue ⇒
                val defSite = definitionSites(method, pc)
                state.setOrUpdatePointsToSet(
                    defSite,
                    if (const.isNullExpr) emptyPointsToSet
                    // note, this is wrong for alias analyses
                    else createPointsToSet(pc, state.method, const.tpe.asObjectType)
                )

            case Assignment(pc, _, idc: InvokedynamicFunctionCall[_]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case ExprStmt(pc, idc: InvokedynamicFunctionCall[V]) ⇒
                state.addIncompletePointsToInfo(pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case idc: InvokedynamicMethodCall[_] ⇒
                state.addIncompletePointsToInfo(idc.pc)
                logOnce(
                    Warn("analysis - points-to analysis", s"unresolved invokedynamic: $idc")
                )

            case Assignment(_, DVar(av: IsSArrayValue, _), _) if av.theUpperTypeBound.elementType.isObjectType ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case Assignment(_, DVar(rv: IsReferenceValue, _), _) if !rv.isInstanceOf[IsSArrayValue] ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case call: Call[_] ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], call.pc)

            case ExprStmt(pc, call: Call[_]) ⇒
                handleCall(call.asInstanceOf[Call[DUVar[ValueInformation]]], pc)

            case ArrayStore(_, UVar(av: IsSArrayValue, _), _, UVar(_, defSites)) if av.theUpperTypeBound.elementType.isReferenceType ⇒
                val arrayBaseType = av.theUpperTypeBound.elementType
                state.setOrUpdatePointsToSet(
                    arrayBaseType, currentPointsTo(arrayBaseType, defSites)
                )

            case PutField(pc, declaringClass, name, fieldType: ObjectType, _, UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined) {
                    state.setOrUpdatePointsToSet(
                        fieldOpt.get, currentPointsTo(fieldOpt.get, defSites)
                    )
                } else {
                    state.addIncompletePointsToInfo(pc)
                }

            case PutStatic(pc, declaringClass, name, fieldType: ObjectType, UVar(_, defSites)) ⇒
                val fieldOpt = p.resolveFieldReference(declaringClass, name, fieldType)
                if (fieldOpt.isDefined)
                    state.setOrUpdatePointsToSet(
                        fieldOpt.get, currentPointsTo(fieldOpt.get, defSites)
                    )
                else {
                    state.addIncompletePointsToInfo(pc)
                }

            case ReturnValue(_, UVar(_: IsReferenceValue, defSites)) ⇒
                state.setOrUpdatePointsToSet(
                    state.method, currentPointsTo(state.method, defSites)
                )

            case _ ⇒
        }

        // todo: we have to handle the exceptions that might be thrown by this method

        returnResult(0)
    }

    /**
     * The continuation function. It handles updates for the methods tac, callees and for points-to
     * sets.
     */
    private[this] def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
                state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
                doProcessMethod(state)

            case UBP(_: TACAI) ⇒
                InterimPartialResult(Some(eps), c(state))

            case UBP(callees: Callees) ⇒
                // todo instead of rerunning the complete analysis, get new calls for all pcs only

                state.updateCalleesDependee(eps.asInstanceOf[EPS[DeclaredMethod, Callees]])
                doProcessMethod(state)

            case UBPS(pointsTo: PointsToSet @unchecked, isFinal) ⇒
                val oldEOptP = state.getPointsToProperty(eps.e)
                val seenElements = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0

                for (depender ← state.dependersOf(eps.e)) {
                    state.setPointsToSet(depender, pointsTo)
                }

                if (isFinal) {
                    state.removePointsToDependee(eps.e)
                } else {
                    state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsToSet]])
                }

                returnResult(seenElements)(state)
        }
    }

    /**
     * Constructs the [[org.opalj.fpcf.PropertyComputationResult]] associated with the state.
     */
    @inline private[this] def returnResult(seenElements: Int)(
        implicit
        state: State
    ): ProperPropertyComputationResult = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        for ((e, pointsToSet) ← state.pointsToSetsIterator) {
            if (pointsToSet ne emptyPointsToSet) {
                results += PartialResult[Entity, PointsToSetLike[_, _, PointsToSet]](e, pointsToPropertyKey, {

                    case _: EPK[Entity, _] ⇒
                        if (usefull % 50000 == 0)
                            println(s"usefull partial result #${usefull} - ${pointsToSet.numElements}")
                        usefull += 1
                        Some(InterimEUBP(e, pointsToSet))

                    case UBP(ub: PointsToSet @unchecked) ⇒
                        val newPointsTo = ub.included(pointsToSet, seenElements)
                        if (newPointsTo ne ub) {
                            if (usefull % 50000 == 0)
                                println(s"usefull partial result #${usefull} - ${pointsToSet.numElements}")
                            usefull += 1
                            Some(InterimEUBP(e, newPointsTo))
                        } else {
                            if (count % 50000 == 0)
                                println(s"useless partial result #${count} - ${pointsToSet.numElements}")
                            count += 1
                            None
                        }

                    case eOptP ⇒
                        throw new IllegalArgumentException(s"unexpected eOptP: $eOptP")
                })
            }
        }

        if (state.hasOpenDependencies) results += InterimPartialResult(state.dependees, c(state))

        state.clearPointsToSet()

        Results(results)
    }
}
