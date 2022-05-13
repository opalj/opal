/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

/**
 * Models the behavior for `java.security.AccessController.doPrivileged*`.
 *
 * On each call of the concrete [[doPrivilegedMethod]] method it will call the
 * [[declaredRunMethod]] upon its first parameter and returns the result of this call.
 *
 * For each such call, the analysis will add an indirect call to the call graph.
 *
 * TODO: The current implementation won't work if the JDK is not included. In order to perform the
 * analysis even if the method is a [[org.opalj.br.VirtualDeclaredMethod]], the
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s must also be present for those methods.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class DoPrivilegedMethodAnalysis private[cg] (
        final val doPrivilegedMethod: DeclaredMethod,
        final val declaredRunMethod:  DeclaredMethod,
        override val project:         SomeProject
) extends TACAIBasedAPIBasedAnalysis with TypeConsumerAnalysis {

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
        val indirectCalls = new IndirectCalls()

        if (params.nonEmpty && params.head.isDefined) {
            val param = params.head.get.asVar

            implicit val state: CGState[ContextType] = new CGState[ContextType](
                callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
            )

            val thisActual = persistentUVar(param)(state.tac.stmts)

            typeProvider.foreachType(
                param,
                typeProvider.typesProperty(
                    param, callerContext, callPC.asInstanceOf[Entity], tac.stmts
                )
            ) { tpe => handleType(tpe, callerContext, callPC, thisActual, indirectCalls) }

            returnResult(param, thisActual, indirectCalls)
        } else {
            indirectCalls.addIncompleteCallSite(callPC)
            Results(indirectCalls.partialResults(callerContext))
        }
    }

    def returnResult(
        thisVar:    V,
        thisActual: Some[(ValueInformation, IntTrieSet)],
        calls:      IndirectCalls
    )(implicit state: CGState[ContextType]): ProperPropertyComputationResult = {

        val partialResults = calls.partialResults(state.callContext)
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state, thisVar, thisActual)),
                partialResults
            )
        else
            Results(partialResults)
    }

    private[this] def handleType(
        tpe:               ReferenceType,
        callContext:       ContextType,
        callPC:            Int,
        thisActual:        Some[(ValueInformation, IntTrieSet)],
        calleesAndCallers: IndirectCalls
    ): Unit = {
        val callR = p.instanceCall(
            callContext.method.declaringClassType,
            tpe,
            declaredRunMethod.name,
            declaredRunMethod.descriptor
        )
        if (callR.hasValue) {
            val tgtMethod = declaredMethods(callR.value)
            calleesAndCallers.addCall(
                callContext,
                callPC,
                typeProvider.expandContext(callContext, tgtMethod, callPC),
                Seq.empty,
                thisActual
            )
        } else {
            calleesAndCallers.addIncompleteCallSite(callPC)
        }
    }

    def c(
        state:      CGState[ContextType],
        thisVar:    V,
        thisActual: Some[(ValueInformation, IntTrieSet)]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        val pc = state.dependersOf(eps.toEPK).head.asInstanceOf[Int]

        // ensures, that we only add new vm reachable methods
        val indirectCalls = new IndirectCalls()

        typeProvider.continuation(thisVar, eps.asInstanceOf[EPS[Entity, PropertyType]]) { newType =>
            handleType(newType, state.callContext, pc, thisActual, indirectCalls)
        }(state)

        if (eps.isFinal) {
            state.removeDependee(eps.toEPK)
        } else {
            state.updateDependency(eps)
        }

        returnResult(thisVar, thisActual, indirectCalls)(state)
    }

    override val apiMethod: DeclaredMethod = doPrivilegedMethod
}

class DoPrivilegedCGAnalysis private[cg] (
        final val project: SomeProject
) extends FPCFAnalysis {

    def analyze(p: SomeProject): PropertyComputationResult = {
        var analyses: List[DoPrivilegedMethodAnalysis] = Nil

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
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged1, runMethod, p)

        val doPrivileged2 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                ArraySeq(privilegedActionType, accessControlContextType),
                ObjectType.Object
            )
        )
        if (doPrivileged2.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged2, runMethod, p)

        val doPrivileged3 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                ArraySeq(privilegedActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivileged3.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged3, runMethod, p)

        val doPrivileged4 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(privilegedExceptionActionType, ObjectType.Object)
        )
        if (doPrivileged4.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged4, runMethod, p)

        val doPrivileged5 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                ArraySeq(privilegedExceptionActionType, accessControlContextType),
                ObjectType.Object
            )
        )
        if (doPrivileged5.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged5, runMethod, p)

        val doPrivileged6 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(
                ArraySeq(privilegedExceptionActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivileged6.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivileged6, runMethod, p)

        val doPrivilegedWithCombiner1 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(privilegedActionType, ObjectType.Object)
        )
        if (doPrivilegedWithCombiner1.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivilegedWithCombiner1, runMethod, p)

        val doPrivilegedWithCombiner2 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                ArraySeq(privilegedActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner2.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivilegedWithCombiner2, runMethod, p)

        val doPrivilegedWithCombiner3 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                ArraySeq(privilegedExceptionActionType),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner3.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivilegedWithCombiner3, runMethod, p)

        val doPrivilegedWithCombiner4 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(
                ArraySeq(privilegedExceptionActionType, accessControlContextType, permissionsArray),
                ObjectType.Object
            )
        )
        if (doPrivilegedWithCombiner4.hasSingleDefinedMethod)
            analyses ::= new DoPrivilegedMethodAnalysis(doPrivilegedWithCombiner4, runMethod, p)

        Results(analyses.iterator.map(_.registerAPIMethod())) //analyze()))
    }
}

object DoPrivilegedAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(Callers, Callees)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): DoPrivilegedCGAnalysis = {
        val analysis = new DoPrivilegedCGAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.analyze)
        analysis
    }
}