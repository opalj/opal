/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.Entity
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
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
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
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase

/**
 * On each call of the [[sourceMethod*]] it will call the [[declaredTargetMethod*]] upon its first
 * parameter and returns the result of this call.
 * This analysis manages the entries for [[Callees]] and
 * [[Callers]] as well as the
 * [[PointsToSet]] mappings.
 *
 * TODO: This analysis is very specific to the points-to analysis. It should also work for the other
 * analyses.
 *
 * TODO: The current implementation won't work if the JDK is not included. In order to perform the
 * analysis even if the method is a [[org.opalj.br.VirtualDeclaredMethod]], the
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s must also be present for those methods.
 *
 * TODO: This analysis produces direct calls from AccessController.doPrivileged to the corresponding
 * run method. It should probably instead produce indirect calls from the caller of doPrivileged to
 * the individual run method.
 *
 * @author Florian Kuebler
 */
abstract class AbstractDoPrivilegedPointsToCGAnalysis private[cg] (
        final val sourceMethod:         DeclaredMethod,
        final val declaredTargetMethod: DeclaredMethod,
        final val project:              SomeProject
) extends PointsToAnalysisBase {
    private[this] val declaredMethods = p.get(DeclaredMethodsKey)

    def analyze(): ProperPropertyComputationResult = {
        // take the first parameter
        val fps = formalParameters(sourceMethod)
        val fp = fps(1)
        val pointsToParam = ps(fp, pointsToPropertyKey)

        Results(methodMapping(pointsToParam, 0))
    }

    def continuationForParameterValue(
        seenElements: Int
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case EUBP(_: VirtualFormalParameter, _: PointsToSet @unchecked) ⇒
            methodMapping(
                eps.asInstanceOf[EPS[VirtualFormalParameter, PointsToSet]],
                seenElements
            )
        case _ ⇒
            throw new IllegalStateException(s"unexpected update $eps")
    }

    def methodMapping(
        dependeeEOptP: EOptionP[VirtualFormalParameter, PointsToSet],
        seenElements:  Int
    ): ProperPropertyComputationResult = {

        val calls = new DirectCalls()
        var results: List[ProperPropertyComputationResult] = Nil

        val newSeenElements = if (dependeeEOptP.hasUBP) {
            val dependeePointsTo = dependeeEOptP.ub
            dependeePointsTo.types.foreach { t ⇒
                val callR = p.instanceCall(
                    sourceMethod.declaringClassType,
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
                    results ::= PartialResult[VirtualFormalParameter, PointsToSet](
                        tgtThis,
                        pointsToPropertyKey,
                        {
                            case UBP(oldPointsToUB: PointsToSet @unchecked) ⇒
                                val newPointsToUB = oldPointsToUB.included(
                                    dependeePointsTo,
                                    seenElements,
                                    { x: ReferenceType ⇒ x == t }
                                )
                                if (newPointsToUB eq oldPointsToUB) {
                                    None
                                } else {
                                    Some(InterimEUBP(tgtThis, newPointsToUB))
                                }

                            case _: EPK[VirtualFormalParameter, PointsToSet] ⇒
                                val newPointsToUB = emptyPointsToSet.included(
                                    dependeePointsTo,
                                    seenElements,
                                    { x: ReferenceType ⇒ x == t }
                                )
                                Some(InterimEUBP(
                                    tgtThis,
                                    newPointsToUB
                                ))
                        }
                    )

                    // 3. Map the return value back to the source method
                    val returnPointsTo = ps(tgtMethod, pointsToPropertyKey)
                    results ::= returnMapping(returnPointsTo, 0)

                } else {
                    calls.addIncompleteCallSite(0)
                }
            }
            dependeePointsTo.numElements
        } else {
            0
        }

        // Must initialize the return points to set if it is not yet initialized in order to avoid
        // the property store to set a fallback value while triggering a partial update.
        if (newSeenElements == 0) {
            results ::= PartialResult[DeclaredMethod, PointsToSet](
                sourceMethod,
                pointsToPropertyKey,
                {
                    case UBP(_: PointsToSet @unchecked) ⇒
                        None

                    case _: EPK[_, _] ⇒
                        Some(InterimEUBP(sourceMethod, emptyPointsToSet))
                }
            )
        }

        if (dependeeEOptP.isRefinable) {
            results ::= InterimPartialResult(
                Some(dependeeEOptP), continuationForParameterValue(newSeenElements)
            )
        }

        results ++= calls.partialResults(sourceMethod)

        Results(results)
    }

    def returnMapping(
        returnPointsTo: EOptionP[DeclaredMethod, PointsToSet],
        seenElements:   Int
    ): ProperPropertyComputationResult = {
        var results: List[ProperPropertyComputationResult] = Nil
        val newSeenElements = if (returnPointsTo.hasUBP) {
            val returnPointsToUB = returnPointsTo.ub
            results ::= PartialResult[DeclaredMethod, PointsToSet](
                sourceMethod,
                pointsToPropertyKey,
                {
                    case UBP(ub: PointsToSet @unchecked) ⇒
                        val newUB = ub.included(returnPointsToUB, seenElements)
                        if (newUB eq ub) {
                            None
                        } else {
                            Some(InterimEUBP(sourceMethod, newUB))
                        }

                    case _: EPK[_, _] ⇒
                        Some(InterimEUBP(sourceMethod, returnPointsToUB))
                }
            )
            returnPointsToUB.numElements
        } else {
            0
        }

        if (returnPointsTo.isRefinable) {
            results ::= InterimPartialResult(
                Some(returnPointsTo), continuationForReturnValue(newSeenElements)
            )
        }

        Results(results)
    }

    def continuationForReturnValue(
        seenElements: Int
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        // join the return values of all invoked methods
        case EUBP(_: DeclaredMethod, _: PointsToSet @unchecked) ⇒
            returnMapping(
                eps.asInstanceOf[EPS[DeclaredMethod, PointsToSet]], seenElements
            )

        case _ ⇒
            throw new IllegalStateException(s"unexpected update $eps")
    }
}

class DoPrivilegedPointsToCGAnalysis private[cg] (
        final val project: SomeProject
) extends PointsToAnalysisBase with AllocationSiteBasedAnalysis { self ⇒

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
        ): PointsToSet =
            self.createPointsToSet(pc, declaredMethod, allocatedType, isConstant, isEmptyArray)

        override protected[this] def currentPointsTo(
            depender:   DependerType,
            dependee:   Entity,
            typeFilter: ReferenceType ⇒ Boolean
        )(implicit state: State): PointsToSet =
            self.currentPointsTo(depender, dependee, typeFilter)
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

        Results(analyses.iterator.map(_.analyze()))
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

