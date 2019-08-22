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
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.IntegerType
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.BooleanType
import org.opalj.br.ReferenceType
import org.opalj.br.VoidType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TheTACAI

abstract class AllocationSiteBasedTamiFlexPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToAnalysisBase { self ⇒

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
        ): PointsToSet =
            self.createPointsToSet(pc, declaredMethod, allocatedType, isConstant, isEmptyArray)

        override protected[this] def currentPointsTo(
            depender:   DependerType,
            dependee:   Entity,
            typeFilter: ReferenceType ⇒ Boolean
        )(implicit state: State): PointsToSet =
            self.currentPointsTo(depender, dependee, typeFilter)
    }

    val declaredMethods = project.get(DeclaredMethodsKey)

    def process(p: SomeProject): PropertyComputationResult = {
        val analyses: List[APIBasedAnalysis] = List(
            new TamiFlexPointsToArrayGetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToArraySetAnalysis(project) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    TamiFlexPointsToAnalysis.ArrayT, "", TamiFlexPointsToAnalysis.ArrayT,
                    "newInstance",
                    MethodDescriptor(RefArray(ObjectType.Class, IntegerType), ObjectType.Object)
                ),
                "Array.newInstance"
            ) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    TamiFlexPointsToAnalysis.ArrayT, "", TamiFlexPointsToAnalysis.ArrayT,
                    "newInstance",
                    MethodDescriptor(
                        RefArray(ObjectType.Class, ArrayType(IntegerType)), ObjectType.Object
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
                    MethodDescriptor(RefArray(ObjectType.String, BooleanType, ObjectType("java/lang/ClassLoader")), ObjectType.Class)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getField", TamiFlexPointsToAnalysis.FieldT)() with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredField", TamiFlexPointsToAnalysis.FieldT)() with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getFields", TamiFlexPointsToAnalysis.FieldT) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredFields", TamiFlexPointsToAnalysis.FieldT) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getConstructor", TamiFlexPointsToAnalysis.ConstructorT)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), TamiFlexPointsToAnalysis.ConstructorT)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredConstructor", TamiFlexPointsToAnalysis.ConstructorT)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredConstructor",
                    MethodDescriptor(ArrayType(ObjectType.Class), TamiFlexPointsToAnalysis.ConstructorT)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getConstructors", TamiFlexPointsToAnalysis.ConstructorT) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredConstructors", TamiFlexPointsToAnalysis.ConstructorT) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getMethod", TamiFlexPointsToAnalysis.MethodT)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getMethod",
                    MethodDescriptor(RefArray(ObjectType.String, ArrayType(ObjectType.Class)), TamiFlexPointsToAnalysis.MethodT)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMemberAnalysis(project, "getDeclaredMethod", TamiFlexPointsToAnalysis.MethodT)(
                declaredMethods(
                    ObjectType.Class, "", ObjectType.Class,
                    "getDeclaredMethod",
                    MethodDescriptor(RefArray(ObjectType.String, ArrayType(ObjectType.Class)), TamiFlexPointsToAnalysis.MethodT)
                )
            ) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getMethods", TamiFlexPointsToAnalysis.MethodT) with PointsToBase,
            new TamiFlexPointsToClassGetMembersAnalysis(project, "getDeclaredMethods", TamiFlexPointsToAnalysis.MethodT) with PointsToBase,
            new TamiFlexPointsToNewInstanceAnalysis(
                project,
                declaredMethods(
                    TamiFlexPointsToAnalysis.ConstructorT, "", TamiFlexPointsToAnalysis.ConstructorT,
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

object AllocationSiteBasedTamiFlexPointsToAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        AllocationSitePointsToSet
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        AllocationSitePointsToSet
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AllocationSiteBasedTamiFlexPointsToAnalysis(p) with AllocationSiteBasedAnalysis
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}

abstract class TamiFlexPointsToArrayGetAnalysis( final val project: SomeProject)
        extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        TamiFlexPointsToAnalysis.ArrayT,
        "",
        TamiFlexPointsToAnalysis.ArrayT,
        "get",
        MethodDescriptor(RefArray(ObjectType.Object, IntegerType), ObjectType.Object)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theArray = params.head
        if (theArray.isDefined) {
            implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet](
                caller, FinalEP(caller.definedMethod, TheTACAI(tac))
            )

            val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val arrays = tamiFlexLogData.classes(caller, "Array.get*", line)
            for (array ← arrays) {
                handleArrayLoad(array.asArrayType, pc, theArray.get.asVar.definedBy)
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

abstract class TamiFlexPointsToArraySetAnalysis( final val project: SomeProject)
        extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        TamiFlexPointsToAnalysis.ArrayT,
        "",
        TamiFlexPointsToAnalysis.ArrayT,
        "set",
        MethodDescriptor(RefArray(ObjectType.Object, IntegerType, ObjectType.Object), VoidType)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        caller:          DefinedMethod,
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
            implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet](
                caller, FinalEP(caller.definedMethod, TheTACAI(tac))
            )

            val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val arrays = tamiFlexLogData.classes(caller, "Array.set*", line)
            for (array ← arrays) {
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
        caller:   DefinedMethod,
        pc:       Int,
        isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val state: State = new PointsToAnalysisState[ElementType, PointsToSet](null, null)

        val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val allocatedTypes = tamiFlexLogData.classes(caller, key, line)
        val defSite = definitionSites(caller.definedMethod, pc)
        for (allocatedType ← allocatedTypes)
            state.includeSharedPointsToSet(
                defSite,
                createPointsToSet(pc, caller, allocatedType, isConstant = false)
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
        caller:   DefinedMethod,
        pc:       Int,
        isDirect: Boolean
    ): ProperPropertyComputationResult = {

        val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val members = memberType match {
            case ObjectType.Class ⇒
                tamiFlexLogData.classes(caller, s"Class.$method", line)
            case TamiFlexPointsToAnalysis.FieldT ⇒
                tamiFlexLogData.fields(caller, s"Class.$method", line)
            case TamiFlexPointsToAnalysis.MethodT | TamiFlexPointsToAnalysis.ConstructorT ⇒
                tamiFlexLogData.methods(caller, s"Class.$method", line)
        }
        if (members.nonEmpty) {
            val state: State = new PointsToAnalysisState[ElementType, PointsToSet](null, null)

            state.includeSharedPointsToSet(
                definitionSites(caller.definedMethod, pc),
                createPointsToSet(pc, caller, memberType, isConstant = false),
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
        caller:   DefinedMethod,
        pc:       Int,
        isDirect: Boolean
    ): ProperPropertyComputationResult = {

        val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val classTypes = tamiFlexLogData.classes(caller, s"Class.$method", line)
        if (classTypes.nonEmpty) {
            val state: State = new PointsToAnalysisState[ElementType, PointsToSet](null, null)
            state.includeSharedPointsToSet(
                definitionSites(caller.definedMethod, pc),
                createPointsToSet(
                    pc, caller, ArrayType(memberType), isConstant = false
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

abstract class TamiFlexPointsToFieldGetAnalysis( final val project: SomeProject)
        extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        TamiFlexPointsToAnalysis.FieldT,
        "",
        TamiFlexPointsToAnalysis.FieldT,
        "get",
        MethodDescriptor(ObjectType.Object, ObjectType.Object)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {

        val theObject = params.head
        implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet](
            caller, FinalEP(caller.definedMethod, TheTACAI(tac))
        )

        val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
        val fields = tamiFlexLogData.fields(state.method, "Field.get*", line)
        for (field ← fields) {
            if (field.isStatic) {
                handleGetStatic(field, pc)
            } else if (theObject.isDefined) {
                handleGetField(RealField(field), pc, theObject.get.asVar.definedBy)
            }
        }

        Results(createResults(state))
    }
}

abstract class TamiFlexPointsToFieldSetAnalysis( final val project: SomeProject)
        extends PointsToAnalysisBase with TACAIBasedAPIBasedAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        TamiFlexPointsToAnalysis.FieldT,
        "",
        TamiFlexPointsToAnalysis.FieldT,
        "set",
        MethodDescriptor(RefArray(ObjectType.Object, ObjectType.Object), VoidType)
    )

    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        caller:          DefinedMethod,
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
            implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet](
                caller, FinalEP(caller.definedMethod, TheTACAI(tac))
            )

            val line = caller.definedMethod.body.get.lineNumber(pc).getOrElse(-1)
            val fields = tamiFlexLogData.fields(state.method, "Field.set*", line)
            for (field ← fields) {
                if (field.isStatic) {
                    handlePutStatic(field, storeVal.get.asVar.definedBy)
                } else if (theObject.isDefined) {
                    handlePutField(
                        RealField(field), theObject.get.asVar.definedBy, storeVal.get.asVar.definedBy
                    )
                }
            }

            Results(createResults(state))
        } else {
            Results()
        }
    }
}

object TamiFlexPointsToAnalysis {
    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val ArrayT = ObjectType("java/lang/reflect/Array")
    val FieldT = ObjectType("java/lang/reflect/Field")
    val MethodT = ObjectType("java/lang/reflect/Method")
}