/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.IntegerType
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.BooleanType
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

/**
 * Handles the effect of tamiflex logs for the points-to sets.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
abstract class TamiFlexPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToAnalysisBase { self =>

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

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new TamiFlexPointsToArrayGetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToArraySetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ObjectType.Array, "", ObjectType.Array,
                    "newInstance",
                    MethodDescriptor(ArraySeq(ObjectType.Class, IntegerType), ObjectType.Object)
                ),
                "Array.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ObjectType.Array, "", ObjectType.Array,
                    "newInstance",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ArrayType(IntegerType)), ObjectType.Object
                    )
                ),
                "Array.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "newInstance",
                    MethodDescriptor.JustReturnsObject
                ),
                "Class.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "forName", ObjectType.Class)() with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "forName", ObjectType.Class)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "forName",
                    MethodDescriptor(ArraySeq(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")), ObjectType.Class)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getField", ObjectType.Field)() with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredField", ObjectType.Field)() with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getFields", ObjectType.Field) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredFields", ObjectType.Field) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getConstructor", ObjectType.Constructor)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredConstructor", ObjectType.Constructor)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), ObjectType.Constructor)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getConstructors", ObjectType.Constructor) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredConstructors", ObjectType.Constructor) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getMethod", ObjectType.Method)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getMethod",
                    MethodDescriptor(ArraySeq(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredMethod", ObjectType.Method)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(ArraySeq(ObjectType.String, ArrayType(ObjectType.Class)), ObjectType.Method)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getMethods", ObjectType.Method) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredMethods", ObjectType.Method) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ObjectType.Constructor, "", ObjectType.Constructor,
                    "newInstance",
                    MethodDescriptor(
                        ArrayType.ArrayOfObject, ObjectType.Object
                    )
                ),
                "Constructor.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToFieldGetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToFieldSetAnalysis(project) with PointsToBase
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

trait TamiFlexPointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    val propertyKind: PropertyMetaInformation
    val createAnalysis: SomeProject => TamiFlexPointsToAnalysis

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        VirtualFormalParametersKey,
        DefinitionSitesKey,
        TamiFlexKey,
        TypeProviderKey
    )

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, propertyKind)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

}

object TypeBasedTamiFlexPointsToAnalysisScheduler extends TamiFlexPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => TamiFlexPointsToAnalysis =
        new TamiFlexPointsToAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedTamiFlexPointsToAnalysisScheduler
    extends TamiFlexPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => TamiFlexPointsToAnalysis =
        new TamiFlexPointsToAnalysis(_) with AllocationSiteBasedAnalysis
}

abstract class TamiFlexPointsToArrayGetAnalysis(
        final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Array,
        "",
        ObjectType.Array,
        "get",
        MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType), ObjectType.Object)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theArray = params.head
        if (theArray.isDefined) {
            implicit val state: State =
                new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                    callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
                )

            val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val arrays = tamiFlexLogData.classes(callerContext.method, "Array.get*", line)
            for (array <- arrays) {
                handleArrayLoad(array.asArrayType, pc, theArray.get.asVar.definedBy)
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

abstract class TamiFlexPointsToArraySetAnalysis(
        final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Array,
        "",
        ObjectType.Array,
        "set",
        MethodDescriptor(ArraySeq(ObjectType.Object, IntegerType, ObjectType.Object), VoidType)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theArray = params.head
        val storeVal = params(2)
        if (theArray.isDefined && storeVal.isDefined) {
            implicit val state: State =
                new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                    callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
                )

            val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val arrays = tamiFlexLogData.classes(callerContext.method, "Array.set*", line)
            for (array <- arrays) {
                handleArrayStore(
                    array.asArrayType, theArray.get.asVar.definedBy, storeVal.get.asVar.definedBy
                )
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

abstract class TamiFlexPointsToNewInstanceAnalysis(
        final val project:      SomeProject,
        override val apiMethod: DeclaredMethod,
        val key:                String
) extends PointsToAnalysisBase with APIBasedAnalysis {

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)

        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val allocatedTypes = tamiFlexLogData.classes(callerContext.method, key, line)
        val defSite = getDefSite(pc)
        for (allocatedType <- allocatedTypes)
            state.includeSharedPointsToSet(
                defSite,
                createPointsToSet(pc, callerContext, allocatedType, isConstant = false)
            )

        Results(createResults(state))
    }
}

abstract class TamiFlexPointsToClassGetMemberAnalysis(
        final val project: SomeProject,
        val method:        String,
        val memberType:    ObjectType
)(
        override val apiMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(
            ObjectType.Class, "", ObjectType.Class,
            method,
            MethodDescriptor(ObjectType.String, memberType)
        )
) extends PointsToAnalysisBase with APIBasedAnalysis {

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {
        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)

        val members = memberType match {
            case ObjectType.Class =>
                tamiFlexLogData.classes(callerContext.method, s"Class.$method", line)
            case ObjectType.Field =>
                tamiFlexLogData.fields(callerContext.method, s"Class.$method", line)
            case ObjectType.Method | ObjectType.Constructor =>
                tamiFlexLogData.methods(callerContext.method, s"Class.$method", line)
        }
        if (members.nonEmpty) {
            implicit val state: State =
                new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)

            state.includeSharedPointsToSet(
                getDefSite(pc),
                createPointsToSet(pc, callerContext, memberType, isConstant = false),
                PointsToSetLike.noFilter
            )

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

abstract class TamiFlexPointsToClassGetMembersAnalysis(
        final val project: SomeProject,
        method:            String,
        val memberType:    ObjectType
) extends PointsToAnalysisBase with APIBasedAnalysis {

    override val apiMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(
        ObjectType.Class, "", ObjectType.Class,
        method,
        MethodDescriptor.withNoArgs(ArrayType(memberType))
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {

        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val classTypes = tamiFlexLogData.classes(callerContext.method, s"Class.$method", line)
        if (classTypes.nonEmpty) {
            implicit val state: State =
                new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callerContext, null)
            state.includeSharedPointsToSet(
                getDefSite(pc),
                createPointsToSet(
                    pc, callerContext, ArrayType(memberType), isConstant = false
                ),
                PointsToSetLike.noFilter
            )
            // todo store something into the array

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

abstract class TamiFlexPointsToFieldGetAnalysis(
        final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Field,
        "",
        ObjectType.Field,
        "get",
        MethodDescriptor(ObjectType.Object, ObjectType.Object)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theObject = params.head
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
            )

        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val fields = tamiFlexLogData.fields(callerContext.method, "Field.get*", line)
        for (field <- fields) {
            if (field.isStatic) {
                handleGetStatic(field, pc)
            } else if (theObject.isDefined) {
                handleGetField(Some(field), pc, theObject.get.asVar.definedBy)
            }
        }

        Results(createResults(state))
    }
}

abstract class TamiFlexPointsToFieldSetAnalysis(
        final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Field,
        "",
        ObjectType.Field,
        "set",
        MethodDescriptor(ArraySeq(ObjectType.Object, ObjectType.Object), VoidType)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theObject = params.head
        val storeVal = params(1)
        if (storeVal.isDefined) {
            implicit val state: State =
                new PointsToAnalysisState[ElementType, PointsToSet, ContextType](
                    callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
                )

            val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val fields = tamiFlexLogData.fields(callerContext.method, "Field.set*", line)
            for (field <- fields) {
                if (field.isStatic) {
                    handlePutStatic(field, storeVal.get.asVar.definedBy)
                } else if (theObject.isDefined) {
                    handlePutField(
                        Some(field), theObject.get.asVar.definedBy, storeVal.get.asVar.definedBy
                    )
                }
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}