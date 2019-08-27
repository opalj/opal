/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.LongType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.BooleanType
import org.opalj.br.IntegerType
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TheTACAI

abstract class UnsafePointsToAnalysis private[pointsto] ( final val project: SomeProject)
        extends PointsToAnalysisBase { self ⇒

    private[this] val UnsafeT = ObjectType("sun/misc/Unsafe")
    val declaredMethods = project.get(DeclaredMethodsKey)

    trait PointsToBase extends AbstractPointsToBasedAnalysis {
        override protected[this]type ElementType = self.ElementType
        override protected[this]type PointsToSet = self.PointsToSet
        override protected[this]type State = self.State
        override protected[this]type DependerType = self.DependerType

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

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new UnsafeGetPointsToAnalysis(
                p,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "getObject",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType), ObjectType.Object)
                )
            ) with PointsToBase,
            new UnsafeGetPointsToAnalysis(
                p,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "getObject",
                    MethodDescriptor(RefArray(ObjectType.Object, IntegerType), ObjectType.Object)
                )
            ) with PointsToBase,
            new UnsafeGetPointsToAnalysis(
                p,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "getObjectVolatile",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType), ObjectType.Object)
                )
            ) with PointsToBase,
            new UnsafePutPointsToAnalysis(
                p, 2,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "putObject",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new UnsafePutPointsToAnalysis(
                p, 2,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "putObject",
                    MethodDescriptor(RefArray(ObjectType.Object, IntegerType, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new UnsafePutPointsToAnalysis(
                p, 2,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "putObjectVolatile",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new UnsafePutPointsToAnalysis(
                p, 2,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "putOrderedObject",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType, ObjectType.Object), VoidType)
                )
            ) with PointsToBase,
            new UnsafePutPointsToAnalysis(
                p, 3,
                declaredMethods(
                    UnsafeT, "", UnsafeT,
                    "compareAndSwapObject",
                    MethodDescriptor(RefArray(ObjectType.Object, LongType, ObjectType.Object, ObjectType.Object), BooleanType)
                )
            ) with PointsToBase
        )

        Results(analyses.map(_.registerAPIMethod()))
    }

}

abstract class UnsafeGetPointsToAnalysis(
        final val project:      SomeProject,
        override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet](
                caller, FinalEP(caller.definedMethod, TheTACAI(tac))
            )

        val theObject = params.head

        if (theObject.isDefined) {
            handleGetField(UnsafeFakeField, pc, theObject.get.asVar.definedBy)
        }

        Results(createResults(state))
    }
}

abstract class UnsafePutPointsToAnalysis(
        final val project:      SomeProject,
        val index:              Int,
        override val apiMethod: DeclaredMethod
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet](
                caller, FinalEP(caller.definedMethod, TheTACAI(tac))
            )

        val baseObject = params.head
        val storeObject = params(index)

        if (baseObject.isDefined && storeObject.isDefined) {
            handlePutField(UnsafeFakeField, baseObject.get.asVar.definedBy, storeObject.get.asVar.definedBy)
        }

        Results(createResults(state))
    }
}

trait UnsafePointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    val propertyKind: PropertyMetaInformation
    val createAnalysis: SomeProject ⇒ UnsafePointsToAnalysis

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}

object TypeBasedUnsafePointsToAnalysisScheduler extends UnsafePointsToAnalysisScheduler {
    override val propertyKind = TypeBasedPointsToSet
    override val createAnalysis: SomeProject ⇒ UnsafePointsToAnalysis =
        new UnsafePointsToAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedUnsafePointsToAnalysisScheduler
        extends UnsafePointsToAnalysisScheduler {
    override val propertyKind = AllocationSitePointsToSet
    override val createAnalysis: SomeProject ⇒ UnsafePointsToAnalysis =
        new UnsafePointsToAnalysis(_) with AllocationSiteBasedAnalysis
}