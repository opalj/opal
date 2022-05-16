/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.ArrayType
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.cg.Callees

/**
 * Introduces object allocations for newInstance reflection methods.
 *
 * @author Dominik Helm
 */
abstract class NewInstanceAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToAnalysisBase { self =>

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this] type ElementType = self.ElementType
        override protected[this] type PointsToSet = self.PointsToSet
        override protected[this] type DependerType = self.DependerType

        override protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet] =
            self.pointsToPropertyKey

        override protected[this] def emptyPointsToSet: PointsToSet = self.emptyPointsToSet

        override protected[this] def createPointsToSet(
            pc:            Int,
            callContext:   ContextType,
            allocatedType: ReferenceType,
            isConstant:    Boolean,
            isEmptyArray:  Boolean
        ): PointsToSet = {
            self.createPointsToSet(
                pc,
                callContext.asInstanceOf[self.ContextType],
                allocatedType,
                isConstant,
                isEmptyArray
            )
        }

        @inline override protected[this] def getTypeOf(element: ElementType): ReferenceType = {
            self.getTypeOf(element)
        }

        @inline override protected[this] def getTypeIdOf(element: ElementType): Int = {
            self.getTypeIdOf(element)
        }

        @inline override protected[this] def isEmptyArray(element: ElementType): Boolean = {
            self.isEmptyArray(element)
        }
    }

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new NewInstanceMethodAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "newInstance",
                    MethodDescriptor.JustReturnsObject
                )
            ) with PointsToBase,
            new NewInstanceMethodAnalysis(
                project,
                declaredMethods(
                    ObjectType.Constructor,
                    "",
                    ObjectType.Constructor,
                    "newInstance",
                    MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                )
            ) with PointsToBase
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

abstract class NewInstanceMethodAnalysis(
        final val project:            SomeProject,
        final override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with APIBasedAnalysis {

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {

        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)

        val defSite = getDefSite(pc)

        val callees = propertyStore(callerContext.method, Callees.key)

        state.addDependee(defSite, callees, PointsToSetLike.noFilter)
        val pointsToSet = if (callees.hasUBP) {
            handleCallees(callees.ub, callerContext, pc, defSite)
        } else {
            emptyPointsToSet
        }

        state.includeSharedPointsToSet(
            defSite,
            pointsToSet,
            PointsToSetLike.noFilter
        )

        Results(createResults(state))
    }

    override def continuationForShared(
        e:         Entity,
        dependees: Map[SomeEPK, (SomeEOptionP, ReferenceType => Boolean)],
        state:     PointsToAnalysisState[ElementType, PointsToSet, ContextType]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(callees: Callees) =>
                val newDependees = updatedDependees(eps, dependees)

                val defSite = e match {
                    case ds: DefinitionSite               => ds
                    case (_: Context, ds: DefinitionSite) => ds
                }

                val pointsToSet = handleCallees(callees, state.callContext, defSite.pc, defSite)

                val results = createPartialResults(
                    e,
                    pointsToSet,
                    newDependees,
                    { old => old.included(pointsToSet) },
                    true
                )(state)

                Results(results)
            case _ => super.continuationForShared(e, dependees, state)(eps)
        }
    }

    def handleCallees(callees: Callees, callerContext: ContextType, pc: Int, defSite: Entity): PointsToSet = {
        var pointsToSet = emptyPointsToSet
        for (callee <- callees.indirectCallees(callerContext, pc) if callee.method.name == "<init>") {
            pointsToSet = pointsToSet.included(
                createPointsToSet(
                    pc,
                    callerContext,
                    callee.method.declaringClassType,
                    isConstant = false
                )
            )
        }
        pointsToSet
    }
}

trait NewInstanceAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject => NewInstanceAnalysis

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey, TypeProviderKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callees, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

object TypeBasedNewInstanceAnalysisScheduler extends NewInstanceAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => NewInstanceAnalysis =
        new NewInstanceAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedNewInstanceAnalysisScheduler extends NewInstanceAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => NewInstanceAnalysis =
        new NewInstanceAnalysis(_) with AllocationSiteBasedAnalysis
}

