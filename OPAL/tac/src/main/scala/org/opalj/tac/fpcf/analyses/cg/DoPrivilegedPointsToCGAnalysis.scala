/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.ReferenceType

/**
 * On each call of the [[sourceMethod*]] it will call the [[declaredTargetMethod*]] upon its first
 * parameter and returns the result of this call.
 * This analysis manages the entries for [[Callees]] and
 * [[Callers]] as well as the
 * [[AllocationSitePointsToSet]] mappings.
 *
 * TODO: This analysis is very specific to the points-to analysis. It should also work for the other
 * analyses.
 *
 * TODO: The current implementation won't work if the JDK is not included. In order to perform the
 * analysis even if the method is a [[org.opalj.br.VirtualDeclaredMethod]], the
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s must also be present for those methods.
 *
 * @author Florian Kuebler
 */
class AbstractDoPrivilegedPointsToCGAnalysis private[cg] (
        final val sourceMethod:         DeclaredMethod,
        final val declaredTargetMethod: DeclaredMethod,
        final val project:              SomeProject
) extends FPCFAnalysis {
    private[this] val formalParameters = p.get(VirtualFormalParametersKey)
    private[this] val declaredMethods = p.get(DeclaredMethodsKey)

    def analyze(): ProperPropertyComputationResult = {
        // take the first parameter
        val fps = formalParameters(sourceMethod)
        val fp = fps(1)
        val pointsToParam = ps(fp, AllocationSitePointsToSet.key)

        Results(methodMapping(pointsToParam, 0, 0))
    }

    def continuationForParameterValue(
        seenElements: Int,
        seenTypes:    Int
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case EUBP(_: VirtualFormalParameter, _: AllocationSitePointsToSet) ⇒
            methodMapping(
                eps.asInstanceOf[EPS[VirtualFormalParameter, AllocationSitePointsToSet]],
                seenElements,
                seenTypes
            )
        case _ ⇒
            throw new IllegalStateException(s"unexpected update $eps")
    }

    def methodMapping(
        dependeeEOptP: EOptionP[VirtualFormalParameter, AllocationSitePointsToSet],
        seenElements:  Int,
        seenTypes:     Int
    ): ProperPropertyComputationResult = {

        val calls = new DirectCalls()
        var results: List[ProperPropertyComputationResult] = Nil

        val (newSeenElements, newSeenTypes) = if (dependeeEOptP.hasUBP) {
            val dependeePointsTo = dependeeEOptP.ub
            dependeePointsTo.types.foreach { t ⇒
                val callR = p.instanceCall(
                    sourceMethod.declaringClassType.asObjectType,
                    t,
                    declaredTargetMethod.name,
                    declaredTargetMethod.descriptor
                )
                if (callR.hasValue) {
                    // 1. Add the call to the specified method.
                    val tgtMethod = declaredMethods(callR.value)
                    calls.addCall(sourceMethod, tgtMethod, 0)

                    // 2. The points-to set of *this* of the target method should contain all
                    // information from the points-to set of the first parameter of the source
                    // method.
                    val tgtThis = formalParameters(tgtMethod)(0)
                    results ::= PartialResult[VirtualFormalParameter, AllocationSitePointsToSet](
                        tgtThis,
                        AllocationSitePointsToSet.key,
                        {
                            case UBP(oldPointsToUB: AllocationSitePointsToSet) ⇒
                                val newPointsToUB = oldPointsToUB.included(
                                    dependeePointsTo,
                                    seenElements,
                                    seenTypes,
                                    { x: ReferenceType ⇒ x == t }
                                )
                                if (newPointsToUB eq oldPointsToUB) {
                                    None
                                } else {
                                    Some(InterimEUBP(tgtThis, newPointsToUB))
                                }

                            case _: EPK[VirtualFormalParameter, AllocationSitePointsToSet] ⇒
                                val newPointsToUB = NoAllocationSites.included(
                                    dependeePointsTo,
                                    seenElements,
                                    seenTypes,
                                    { x: ReferenceType ⇒ x == t }
                                )
                                Some(InterimEUBP(
                                    tgtThis,
                                    newPointsToUB
                                ))
                        }
                    )

                    // 3. Map the return value back to the source method
                    val returnPointsTo = ps(tgtMethod, AllocationSitePointsToSet.key)
                    results ::= returnMapping(returnPointsTo, 0, 0)

                } else {
                    calls.addIncompleteCallSite(0)
                }
            }
            (dependeePointsTo.numElements, dependeePointsTo.numTypes)
        } else {
            (0, 0)
        }

        if (dependeeEOptP.isRefinable) {
            results ::= InterimPartialResult(
                Some(dependeeEOptP), continuationForParameterValue(newSeenElements, newSeenTypes)
            )
        }
        results ++= calls.partialResults(sourceMethod)

        Results(results)
    }

    def returnMapping(
        returnPointsTo: EOptionP[DeclaredMethod, AllocationSitePointsToSet],
        seenElements:   Int,
        seenTypes:      Int
    ): ProperPropertyComputationResult = {
        var results: List[ProperPropertyComputationResult] = Nil
        val (newSeenElements, newSeenTypes) = if (returnPointsTo.hasUBP) {
            val returnPointsToUB = returnPointsTo.ub
            results ::= PartialResult[DeclaredMethod, AllocationSitePointsToSet](
                sourceMethod,
                AllocationSitePointsToSet.key,
                {
                    case UBP(ub: AllocationSitePointsToSet) ⇒
                        val newUB = ub.included(returnPointsToUB, seenElements, seenTypes)
                        if (newUB eq ub) {
                            None
                        } else {
                            Some(InterimEUBP(sourceMethod, newUB))
                        }

                    case _: EPK[_, _] ⇒
                        Some(InterimEUBP(sourceMethod, returnPointsToUB))
                }
            )
            (returnPointsToUB.numElements, returnPointsToUB.numTypes)
        } else {
            (0, 0)
        }

        if (returnPointsTo.isRefinable) {
            results ::= InterimPartialResult(
                Some(returnPointsTo), continuationForReturnValue(newSeenElements, newSeenTypes)
            )
        }

        Results(results)
    }

    def continuationForReturnValue(
        seenElements: Int,
        seenTypes:    Int
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        // join the return values of all invoked methods
        case EUBP(_: DeclaredMethod, _: AllocationSitePointsToSet) ⇒
            returnMapping(
                eps.asInstanceOf[EPS[DeclaredMethod, AllocationSitePointsToSet]],
                seenElements,
                seenTypes
            )

        case _ ⇒
            throw new IllegalStateException(s"unexpected update $eps")
    }
}

class DoPrivilegedPointsToCGAnalysis private[cg] (
        final val project: SomeProject
) extends FPCFAnalysis {
    def analyze(p: SomeProject): PropertyComputationResult = {
        var results: List[ProperPropertyComputationResult] = Nil

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged1, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged2, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged3, runMethod, p).analyze()

        val doPrivileged4 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivileged",
            MethodDescriptor(privilegedExceptionActionType, ObjectType.Object)
        )
        if (doPrivileged4.hasSingleDefinedMethod)
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged4, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged5, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivileged6, runMethod, p).analyze()

        val doPrivilegedWithCombiner1 = declaredMethods(
            accessControllerType,
            "java/security",
            accessControllerType,
            "doPrivilegedWithCombiner",
            MethodDescriptor(privilegedActionType, ObjectType.Object)
        )
        if (doPrivilegedWithCombiner1.hasSingleDefinedMethod)
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner1, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner2, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner3, runMethod, p).analyze()

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
            results ::= new AbstractDoPrivilegedPointsToCGAnalysis(doPrivilegedWithCombiner4, runMethod, p).analyze()

        Results(results)
    }
}

object DoPrivilegedPointsToCGAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

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

