/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.pointsto.PointsToBasedCGState
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * Models the behavior for `java.security.AccessController.doPrivileged*`.
 *
 * On each call of the concrete [[doPrivilegedMethod]] method it will call the
 * [[declaredRunMethod]] upon its first parameter and returns the result of this call.
 *
 * For each such call, the analysis will add an indirect call to the call graph, such that
 * the [[org.opalj.tac.fpcf.analyses.cg.pointsto.AbstractPointsToBasedCallGraphAnalysis]] will
 * map the points-to sets accordingly.
 *
 * TODO: This analysis is very specific to the points-to analysis. It should also work for the other
 * analyses.
 *
 * TODO: The current implementation won't work if the JDK is not included. In order to perform the
 * analysis even if the method is a [[org.opalj.br.VirtualDeclaredMethod]], the
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s must also be present for those methods.
 *
 *
 * @author Florian Kuebler
 */
abstract class AbstractDoPrivilegedPointsToCGAnalysis private[cg] (
        final val doPrivilegedMethod: DeclaredMethod,
        final val declaredRunMethod:  DeclaredMethod,
        final val project:            SomeProject
) extends TACAIBasedAPIBasedAnalysis with AbstractPointsToBasedAnalysis {

    override protected[this] type State = PointsToBasedCGState[PointsToSet]
    override protected[this] type DependerType = CallSite

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val calls: IndirectCalls = new IndirectCalls()
        implicit val state: State = new PointsToBasedCGState[PointsToSet](
            caller, FinalEP(caller.definedMethod, TheTACAI(tac))
        )

        val StaticFunctionCallStatement(call) = tac.stmts(tac.properStmtIndexForPC(pc))

        val actualParamDefSites = call.params.head.asVar.definedBy

        val callSite = CallSite(pc, call.name, call.descriptor, call.declaringClass)

        val pointsToSets = currentPointsToOfDefSites(callSite, actualParamDefSites)

        pointsToSets.foreach(pts ⇒ processNewTypes(call, pts, 0))

        returnResult(call)
    }

    def returnResult(
        call: StaticFunctionCall[V]
    )(implicit calls: IndirectCalls, state: State): ProperPropertyComputationResult = {
        val partialResults = calls.partialResults(state.method)
        if (state.hasPointsToDependees)
            Results(InterimPartialResult(state.dependees, c(state, call)), partialResults)
        else
            Results(partialResults)
    }

    private[this] def processNewTypes(
        call: StaticFunctionCall[V], pts: PointsToSet, seenTypes: Int
    )(implicit state: State, calleesAndCallers: IndirectCalls): Unit = {
        val caller = state.method
        pts.forNewestNTypes(pts.numTypes - seenTypes) { t ⇒
            val callR = p.instanceCall(
                caller.declaringClassType,
                t,
                declaredRunMethod.name,
                declaredRunMethod.descriptor
            )
            if (callR.hasValue) {
                val tgtMethod = declaredMethods(callR.value)
                val thisActual = persistentUVar(call.params.head.asVar)(state.tac.stmts)
                calleesAndCallers.addCall(caller, tgtMethod, call.pc, Seq.empty, thisActual)
            } else {
                calleesAndCallers.addIncompleteCallSite(call.pc)
            }
        }
    }

    private[this] def c(
        state: State,
        call:  StaticFunctionCall[V]
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case EUBPS(e, ub: PointsToSetLike[_, _, _], isFinal) ⇒
            // TODO: shouldn't we just delete the partial results?
            val calls = new IndirectCalls()
            val oldEOptP = state.getPointsToProperty(e)
            val seenTypes = if (oldEOptP.isEPK) 0 else oldEOptP.ub.numTypes
            if (isFinal) {
                state.removePointsToDependee(eps.e)
            } else {
                state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsToSet]])
            }
            processNewTypes(call, ub.asInstanceOf[PointsToSet], seenTypes)(state, calls)
            returnResult(call)(calls, state)
        case _ ⇒ throw new IllegalArgumentException(s"unexpected update $eps")
    }

    override val apiMethod: DeclaredMethod = doPrivilegedMethod
}

class DoPrivilegedPointsToCGAnalysis private[cg] (
        final val project: SomeProject
) extends AllocationSiteBasedAnalysis { self ⇒

    @inline override protected[this] def currentPointsTo(
        depender:   DependerType,
        dependee:   Entity,
        typeFilter: ReferenceType ⇒ Boolean = PointsToSetLike.noFilter
    )(implicit state: State): PointsToSet = {
        if (state.hasPointsToDependee(dependee)) {
            val p2s = state.getPointsToProperty(dependee)

            // It might be the case that there a dependency for that points-to state in the state
            // from another depender.
            if (!state.hasPointsToDependency(depender, dependee)) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
        } else {
            val p2s = propertyStore(dependee, pointsToPropertyKey)
            if (p2s.isRefinable) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
        }
    }

    override protected[this] type State = PointsToBasedCGState[PointsToSet]
    override protected[this] type DependerType = CallSite

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this] type ElementType = self.ElementType
        override protected[this] type PointsToSet = self.PointsToSet
        override protected[this] type State = self.State
        override protected[this] type DependerType = self.DependerType

        override protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet] =
            self.pointsToPropertyKey

        override protected[this] def emptyPointsToSet: PointsToSet = self.emptyPointsToSet

        override protected[this] def createPointsToSet(
            pc:             Int,
            declaredMethod: DeclaredMethod,
            allocatedType:  ReferenceType,
            isConstant:     Boolean,
            isEmptyArray:   Boolean
        ): PointsToSet = {
            self.createPointsToSet(pc, declaredMethod, allocatedType, isConstant, isEmptyArray)
        }

        override protected[this] def currentPointsTo(
            depender:   DependerType,
            dependee:   Entity,
            typeFilter: ReferenceType ⇒ Boolean
        )(implicit state: State): PointsToSet = {
            self.currentPointsTo(depender, dependee, typeFilter)
        }

        override protected[this] def getTypeOf(element: ElementType): ReferenceType = {
            self.getTypeOf(element)
        }
    }

    def analyze(p: SomeProject): PropertyComputationResult = {
        var analyses: List[AbstractDoPrivilegedPointsToCGAnalysis] = Nil

        val accessControllerType = ObjectType("java/security/AccessController")
        val privilegedActionType = ObjectType("java/security/PrivilegedAction")
        val privilegedExceptionActionType = ObjectType("java/security/PrivilegedExceptionAction")
        val accessControlContextType = ObjectType("java/security/AccessControlContext")
        val permissionType = ObjectType("java/security/Permission")
        val permissionsArray = ArrayType(permissionType)

        val declaredMethods = p.get(DeclaredMethodsKey)
        val runMethod = declaredMethods(
            privilegedActionType,
            "java/security",
            privilegedActionType,
            "run",
            MethodDescriptor.JustReturnsObject
        )

        val doPrivileged1 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(privilegedActionType, ObjectType.Object)
        )
        if (doPrivileged1.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged1, runMethod, p) with PointsToBase

        val doPrivileged2 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                RefArray(privilegedActionType, accessControlContextType),
                ObjectType.Object
            )
        )
        if (doPrivileged2.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged2, runMethod, p) with PointsToBase

        val doPrivileged3 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                RefArray(privilegedActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivileged3.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged3, runMethod, p) with PointsToBase

        val doPrivileged4 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(privilegedExceptionActionType, ObjectType.Object)
        )
        if (doPrivileged4.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged4, runMethod, p) with PointsToBase

        val doPrivileged5 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                RefArray(privilegedExceptionActionType, accessControlContextType),
                ObjectType.Object
            )
        )
        if (doPrivileged5.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged5, runMethod, p) with PointsToBase

        val doPrivileged6 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                RefArray(privilegedExceptionActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivileged6.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged6, runMethod, p) with PointsToBase

        val doPrivilegedWithCombiner1 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(privilegedActionType, ObjectType.Object)
        )
        if (doPrivilegedWithCombiner1.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner1, runMethod, p) with PointsToBase

        val doPrivilegedWithCombiner2 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                RefArray(privilegedActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner2.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner2, runMethod, p) with PointsToBase

        val doPrivilegedWithCombiner3 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                RefArray(privilegedExceptionActionType),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner3.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner3, runMethod, p) with PointsToBase

        val doPrivilegedWithCombiner4 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                RefArray(privilegedExceptionActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner4.hasSingleDefinedMethod)
            analyses ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner4, runMethod, p) with PointsToBase

        Results(analyses.iterator.map(_.registerAPIMethod())) //analyze()))
    }
}

object DoPrivilegedPointsToCGAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(AllocationSitePointsToSet))

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(AllocationSitePointsToSet, Callers, Callees)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): DoPrivilegedPointsToCGAnalysis = {
        val analysis = new DoPrivilegedPointsToCGAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.analyze)
        analysis
    }
}

