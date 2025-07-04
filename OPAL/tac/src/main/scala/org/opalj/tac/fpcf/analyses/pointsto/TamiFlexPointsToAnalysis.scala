/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.immutable.ArraySeq
import org.opalj.br.ArrayType
import org.opalj.br.BooleanType
import org.opalj.br.ClassType
import org.opalj.br.DeclaredMethod
import org.opalj.br.IntegerType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.pointsto.TamiFlexKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.tac.fpcf.properties.TheTACAI

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
                    ClassType.Array,
                    "",
                    ClassType.Array,
                    "newInstance",
                    MethodDescriptor(ArraySeq(ClassType.Class, IntegerType), ClassType.Object)
                ),
                "Array.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ClassType.Array,
                    "",
                    ClassType.Array,
                    "newInstance",
                    MethodDescriptor(ArraySeq(ClassType.Class, ArrayType(IntegerType)), ClassType.Object)
                ),
                "Array.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "newInstance",
                    MethodDescriptor.JustReturnsObject
                ),
                "Class.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "forName", ClassType.Class)() with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "forName", ClassType.Class)(
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "forName",
                    MethodDescriptor(
                        ArraySeq(ClassType.String, BooleanType, ClassType("java/lang/ClassLoader")),
                        ClassType.Class
                    )
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getField", ClassType.Field)() with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredField", ClassType.Field)()
                with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getFields", ClassType.Field) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredFields", ClassType.Field)
                with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getConstructor", ClassType.Constructor)(
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getConstructor",
                    MethodDescriptor(ArrayType(ClassType.Class), ClassType.Constructor)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredConstructor", ClassType.Constructor)(
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ClassType.Class), ClassType.Constructor)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getConstructors", ClassType.Constructor)
                with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredConstructors", ClassType.Constructor)
                with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getMethod", ClassType.Method)(
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getMethod",
                    MethodDescriptor(ArraySeq(ClassType.String, ArrayType(ClassType.Class)), ClassType.Method)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredMethod", ClassType.Method)(
                declaredMethods(
                    ClassType.Class,
                    "",
                    ClassType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(ArraySeq(ClassType.String, ArrayType(ClassType.Class)), ClassType.Method)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getMethods", ClassType.Method) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredMethods", ClassType.Method)
                with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    ClassType.Constructor,
                    "",
                    ClassType.Constructor,
                    "newInstance",
                    MethodDescriptor(ArrayType.ArrayOfObject, ClassType.Object)
                ),
                "Constructor.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToFieldGetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToFieldSetAnalysis(project) with PointsToBase
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

trait TamiFlexPointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler with PointsToBasedAnalysisScheduler {

    val propertyKind: PropertyMetaInformation
    val createAnalysis: SomeProject => TamiFlexPointsToAnalysis

    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation :++ Seq(DeclaredMethodsKey, TamiFlexKey)

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
        ClassType.Array,
        "",
        ClassType.Array,
        "get",
        MethodDescriptor(ArraySeq(ClassType.Object, IntegerType), ClassType.Object)
    )

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
                    callerContext,
                    FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
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
        ClassType.Array,
        "",
        ClassType.Array,
        "set",
        MethodDescriptor(ArraySeq(ClassType.Object, IntegerType, ClassType.Object), VoidType)
    )

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
                    callerContext,
                    FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
                )

            val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val arrays = tamiFlexLogData.classes(callerContext.method, "Array.set*", line)
            for (array <- arrays) {
                handleArrayStore(array.asArrayType, theArray.get.asVar.definedBy, storeVal.get.asVar.definedBy)
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

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
    val memberType:    ClassType
)(
    override val apiMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(
        ClassType.Class,
        "",
        ClassType.Class,
        method,
        MethodDescriptor(ClassType.String, memberType)
    )
) extends PointsToAnalysisBase with APIBasedAnalysis {

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

    override def handleNewCaller(
        calleeContext: ContextType,
        callerContext: ContextType,
        pc:            Int,
        isDirect:      Boolean
    ): ProperPropertyComputationResult = {
        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)

        val members = memberType match {
            case ClassType.Class =>
                tamiFlexLogData.classes(callerContext.method, s"Class.$method", line)
            case ClassType.Field =>
                tamiFlexLogData.fields(callerContext.method, s"Class.$method", line)
            case ClassType.Method | ClassType.Constructor =>
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
    val memberType:    ClassType
) extends PointsToAnalysisBase with APIBasedAnalysis {

    override val apiMethod: DeclaredMethod = project.get(DeclaredMethodsKey)(
        ClassType.Class,
        "",
        ClassType.Class,
        method,
        MethodDescriptor.withNoArgs(ArrayType(memberType))
    )

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
                createPointsToSet(pc, callerContext, ArrayType(memberType), isConstant = false),
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
        ClassType.Field,
        "",
        ClassType.Field,
        "get",
        MethodDescriptor(ClassType.Object, ClassType.Object)
    )

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
                callerContext,
                FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
            )

        val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val fields = tamiFlexLogData.fields(callerContext.method, "Field.get*", line)
        for (field <- fields) {
            if (field.isStatic) {
                handleGetStatic(declaredFields(field), pc)
            } else if (theObject.isDefined) {
                handleGetField(Some(declaredFields(field)), pc, theObject.get.asVar.definedBy)
            }
        }

        Results(createResults(state))
    }
}

abstract class TamiFlexPointsToFieldSetAnalysis(
    final val project: SomeProject
) extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ClassType.Field,
        "",
        ClassType.Field,
        "set",
        MethodDescriptor(ArraySeq(ClassType.Object, ClassType.Object), VoidType)
    )

    private[this] final val tamiFlexLogData = project.get(TamiFlexKey)

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
                    callerContext,
                    FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
                )

            val line = callerContext.method.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val fields = tamiFlexLogData.fields(callerContext.method, "Field.set*", line)
            for (field <- fields) {
                if (field.isStatic) {
                    handlePutStatic(declaredFields(field), storeVal.get.asVar.definedBy)
                } else if (theObject.isDefined) {
                    handlePutField(
                        Some(declaredFields(field)),
                        theObject.get.asVar.definedBy,
                        storeVal.get.asVar.definedBy
                    )
                }
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}
