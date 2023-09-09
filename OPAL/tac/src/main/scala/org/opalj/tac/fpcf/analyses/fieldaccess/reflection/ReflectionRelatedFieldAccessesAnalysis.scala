/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.br.BaseType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedFields
import org.opalj.br.DefinedFieldsKey
import org.opalj.br.DoubleType
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.tac.fpcf.analyses.cg.persistentUVar
import org.opalj.br.Field
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.Method
import org.opalj.br.ShortType
import org.opalj.br.VoidType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.fieldaccess.AccessParameter
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.IndirectFieldAccesses
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.TACAIBasedAPIBasedAnalysis
import org.opalj.tac.fpcf.analyses.cg.AllocationsUtil
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeConsumerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.analyses.cg.reflection.StringUtil
import org.opalj.tac.fpcf.analyses.cg.reflection.TypesUtil
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.MatcherUtil.retrieveSuitableMatcher
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

import scala.collection.immutable.ArraySeq

/**
 * @author Maximilian Rüsch
 */
private[reflection] class ReflectionState[ContextType <: Context](
        override val callContext:                  ContextType,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends BaseAnalysisState with TypeIteratorState with TACAIBasedAnalysisState[ContextType]

sealed trait ReflectionAnalysis extends TACAIBasedAPIBasedAnalysis {

    implicit val definedFields: DefinedFields = project.get(DefinedFieldsKey)

    implicit final val HighSoundnessMode: Boolean = {
        val activated = try {
            project.config.getBoolean(ReflectionRelatedFieldAccessesAnalysis.ConfigKey)
        } catch {
            case t: Throwable =>
                logOnce(Error(
                    "analysis configuration - reflection analysis",
                    s"couldn't read: ${ReflectionRelatedFieldAccessesAnalysis.ConfigKey}",
                    t
                ))
                false
        }

        logOnce(Info(
            "analysis configuration",
            "field access reflection analysis uses "+(if (activated) "high soundness mode" else "standard mode")
        ))
        activated
    }

    protected def addFieldRead(
        accessContext:  ContextType,
        accessPC:       Int,
        actualReceiver: Field => AccessReceiver,
        matchers:       Iterable[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        FieldMatching.getPossibleFields(matchers.toSeq).foreach { f =>
            indirectFieldAccesses.addFieldRead(
                accessContext,
                accessPC,
                definedFields(f),
                actualReceiver(f)
            )
        }
    }

    protected def addFieldWrite(
        accessContext:  ContextType,
        accessPC:       Int,
        actualReceiver: Field => AccessReceiver,
        actualParam:    Field => AccessParameter,
        matchers:       Iterable[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        FieldMatching.getPossibleFields(matchers.toSeq).foreach { f =>
            indirectFieldAccesses.addFieldWrite(
                accessContext,
                accessPC,
                definedFields(f),
                actualReceiver(f),
                actualParam(f)
            )
        }
    }
}

sealed trait FieldInstanceBasedReflectiveFieldAccessAnalysis extends ReflectionAnalysis {

    protected def constructArrayDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], arrayVar: V, stmts: Array[Stmt[V]]
    ): Entity

    protected def constructNameDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher],
        nameVar: V, stmts: Array[Stmt[V]], classVar: V, context: ContextType
    ): Entity

    protected def constructClassDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], classVar: V, stmts: Array[Stmt[V]]
    ): Entity

    protected def failure(
        accessPC: Int,
        receiver: AccessReceiver,
        param:    Option[AccessParameter],
        matchers: Set[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit

    protected def handleGetField(
        context:         ContextType,
        accessPC:        Int,
        fieldDefSite:    Int,
        actualReceiver:  AccessReceiver,
        actualParameter: Option[AccessParameter],
        baseMatchers:    Set[FieldMatcher],
        stmts:           Array[Stmt[V]]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Set[FieldMatcher] = {
        def constructClassBasedFieldMatcherForClassVar(
            classVar:                 V,
            currentMatchers:          Set[FieldMatcher],
            onlyFieldsExactlyInClass: Boolean
        ): FieldMatcher = {
            // THIS MATCHER FAILS IF ANY POSSIBLE CLASS IS NOT FINAL TODO wtf
            MatcherUtil.retrieveClassBasedFieldMatcher(
                context,
                classVar,
                constructClassDepender(accessPC, actualReceiver, actualParameter, currentMatchers, classVar, stmts),
                accessPC,
                stmts,
                project,
                () => failure(accessPC, actualReceiver, actualParameter, currentMatchers),
                onlyFieldsExactlyInClass,
            )
        }

        var matchers = baseMatchers
        stmts(fieldDefSite).asAssignment.expr match {
            case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredField" | "getField", _, receiver, params) =>
                val isGetField = call.name == "getField"
                if (isGetField) matchers += PublicFieldMatcher

                if (!matchers.contains(NoFieldsMatcher))
                    matchers += MatcherUtil.retrieveNameBasedFieldMatcher(
                        context,
                        params.head.asVar,
                        constructNameDepender(
                            accessPC, actualReceiver, actualParameter, matchers,
                            params.head.asVar, stmts, receiver.asVar,
                            context
                        ),
                        accessPC,
                        stmts,
                        () => failure(accessPC, actualReceiver, actualParameter, matchers)
                    )

                if (!matchers.contains(NoFieldsMatcher))
                    matchers += constructClassBasedFieldMatcherForClassVar(receiver.asVar, matchers, !isGetField)

            case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredFields" | "getFields", _, receiver, _) =>
                val isGetFields = call.name == "getFields"
                if (isGetFields)
                    matchers += PublicFieldMatcher
                if (!matchers.contains(NoFieldsMatcher))
                    matchers += constructClassBasedFieldMatcherForClassVar(receiver.asVar, matchers, !isGetFields)

            case ArrayLoad(_, _, arrayRef) =>
                val arrayDepender = constructArrayDepender(accessPC, actualReceiver, actualParameter, matchers, arrayRef.asVar, stmts)

                AllocationsUtil.handleAllocations(
                    arrayRef.asVar, context, arrayDepender, stmts, _ eq ObjectType.Field,
                    () => failure(accessPC, actualReceiver, actualParameter, baseMatchers)
                ) { (allocationContext, allocationIndex, stmts) =>
                        matchers ++= handleGetField(
                            allocationContext, accessPC, allocationIndex,
                            actualReceiver, actualParameter,
                            baseMatchers, stmts
                        )
                    }

            case _ =>
                if (HighSoundnessMode) {
                    matchers += AllFieldsMatcher
                } else {
                    indirectFieldAccesses.addIncompleteAccessSite(accessPC)
                    matchers += NoFieldsMatcher
                }
        }

        matchers
    }
}

class FieldGetAnalysis private[analyses] (
        final val project:       SomeProject,
        final val apiMethodName: String,
        final val fieldType:     Option[BaseType] = None
) extends ReflectionAnalysis with TypeConsumerAnalysis with FieldInstanceBasedReflectiveFieldAccessAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Field,
        "",
        ObjectType.Field,
        apiMethodName,
        MethodDescriptor.apply(ObjectType.Object, fieldType.getOrElse(ObjectType.Object))
    )

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        accessPC:        Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
        implicit val state: ReflectionState[ContextType] = new ReflectionState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleFieldRead(callerContext, accessPC, receiverOption.get.asVar, params.head, tac.stmts)
        } else {
            indirectFieldAccesses.addIncompleteAccessSite(accessPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull)
    }

    private def returnResult(fieldVar: V)(
        implicit
        state:                 ReflectionState[ContextType],
        indirectFieldAccesses: IndirectFieldAccesses
    ): ProperPropertyComputationResult = {
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, continuation(fieldVar, state)),
                indirectFieldAccesses.partialResults(state.callContext)
            )
        else
            Results(indirectFieldAccesses.partialResults(state.callContext))
    }

    private case class FieldDepender(pc: Int, receiver: AccessReceiver, matchers: Set[FieldMatcher])
    private case class ArrayDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            matchers:         Set[FieldMatcher],
            arrayVar:         V,
            callerStatements: Array[Stmt[V]]
    )
    private case class NameDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            matchers:         Set[FieldMatcher],
            nameVar:          V,
            callerStatements: Array[Stmt[V]],
            classVar:         V,
            callerContext:    ContextType
    )
    private case class ClassDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            matchers:         Set[FieldMatcher],
            classVar:         V,
            callerStatements: Array[Stmt[V]]
    )

    protected def constructArrayDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], arrayVar: V, stmts: Array[Stmt[V]]
    ): Entity = ArrayDepender(accessPC, receiver, matchers, arrayVar, stmts)

    protected def constructNameDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher],
        nameVar: V, stmts: Array[Stmt[V]], classVar: V, context: ContextType
    ): Entity = NameDepender(accessPC, receiver, matchers, nameVar, stmts, classVar, context)

    protected def constructClassDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], classVar: V, stmts: Array[Stmt[V]]
    ): Entity = ClassDepender(accessPC, receiver, matchers, classVar, stmts)

    // TODO how to test functionality of this? Also can this be deduplicated?
    private def continuation(fieldVar: V, state: ReflectionState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
        implicit val _state: ReflectionState[ContextType] = state

        AllocationsUtil.continuationForAllocation[FieldDepender, ContextType](
            eps, state.callContext, _ => (fieldVar, state.tac.stmts),
            _.isInstanceOf[FieldDepender], data => failure(data.pc, data.receiver, data.matchers)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetField(
                    allocationContext, data.pc, allocationIndex, data.receiver, data.matchers, stmts
                )
                addFieldRead(state.callContext, data.pc, _ => data.receiver, allMatchers)
            }

        AllocationsUtil.continuationForAllocation[NameDepender, ContextType](
            eps, state.callContext, data => (data.nameVar, data.callerStatements),
            _.isInstanceOf[NameDepender],
            data => failure(data.pc, data.receiver, data.matchers)
        ) { (data, _, allocationIndex, stmts) =>
                val name = StringUtil.getString(allocationIndex, stmts)

                val nameMatcher = retrieveSuitableMatcher[Set[String]](
                    name.map(Set(_)),
                    data.pc,
                    v => new NameBasedFieldMatcher(v)
                )

                if (nameMatcher ne NoFieldsMatcher) {
                    val matchers = data.matchers + nameMatcher
                    val allMatchers = matchers +
                        MatcherUtil.retrieveClassBasedFieldMatcher(
                            data.callerContext,
                            data.classVar,
                            ClassDepender(data.pc, data.receiver, matchers, data.classVar, data.callerStatements),
                            data.pc,
                            stmts,
                            project,
                            () => failure(data.pc, data.receiver, matchers),
                            onlyFieldsExactlyInClass = !data.matchers.contains(PublicFieldMatcher)
                        )

                    addFieldRead(state.callContext, data.pc, _ => data.receiver, allMatchers)
                }
            }

        AllocationsUtil.continuationForAllocation[ClassDepender, ContextType](
            eps, state.callContext, data => (data.classVar, data.callerStatements),
            _.isInstanceOf[ClassDepender], data => failure(data.pc, data.receiver, data.matchers)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, data,
                    stmts, project, () => failure(data.pc, data.receiver, data.matchers),
                    onlyObjectTypes = false
                )

                val matchers = data.matchers +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes.map {
                            tpe => if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object
                        }),
                        data.pc,
                        v => new ClassBasedFieldMatcher(v, !data.matchers.contains(PublicFieldMatcher))
                    )

                addFieldRead(state.callContext, data.pc, _ => data.receiver, matchers)
            }

        AllocationsUtil.continuationForAllocation[(ClassDepender, V, Array[Stmt[V]]), ContextType](
            eps, state.callContext, data => (data._2, data._3),
            _.isInstanceOf[(_, _)], data => failure(data._1.pc, data._1.receiver, data._1.matchers)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = false
                )

                val matchers = data._1.matchers +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt.map(Set(_)),
                        data._1.pc,
                        v => new ClassBasedFieldMatcher(v, !data._1.matchers.contains(PublicFieldMatcher))
                    )

                addFieldRead(state.callContext, data._1.pc, _ => data._1.receiver, matchers)
            }

        if (eps.isFinal) state.removeDependee(eps.toEPK)
        else state.updateDependency(eps)

        returnResult(fieldVar)
    }

    private[this] def handleFieldRead(
        callContext:       ContextType,
        accessPC:          Int,
        fieldVar:          V,
        fieldGetParameter: Option[Expr[V]],
        stmts:             Array[Stmt[V]]
    )(implicit state: ReflectionState[ContextType], indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        val fieldGetReceiver: Option[V] = fieldGetParameter.map(_.asVar)

        val baseMatchers = Set(
            MatcherUtil.retrieveSuitableMatcher[V](
                fieldGetReceiver,
                accessPC,
                v => new ActualReceiverBasedFieldMatcher(v.value.asReferenceValue)
            ),
            MatcherUtil.retrieveSuitableNonEssentialMatcher[BaseType](
                fieldType,
                v => new BaseTypeBasedFieldMatcher(v)
            )
        )

        val persistentReceiver = fieldGetReceiver.flatMap(r => persistentUVar(r)(stmts))
        val depender = FieldDepender(accessPC, persistentReceiver, baseMatchers)

        AllocationsUtil.handleAllocations(
            fieldVar, callContext, depender, state.tac.stmts, _ eq ObjectType.Field,
            () => failure(accessPC, persistentReceiver, baseMatchers)
        ) { (allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetField(
                    allocationContext, accessPC, allocationIndex,
                    persistentReceiver,
                    baseMatchers, stmts
                )
                addFieldRead(
                    callContext, accessPC,
                    _ => persistentReceiver,
                    allMatchers
                )
            }
    }

    private def handleGetField(
        context:        ContextType,
        accessPC:       Int,
        fieldDefSite:   Int,
        actualReceiver: AccessReceiver,
        baseMatchers:   Set[FieldMatcher],
        stmts:          Array[Stmt[V]]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Set[FieldMatcher] =
        handleGetField(context, accessPC, fieldDefSite, actualReceiver, None, baseMatchers, stmts)

    private def failure(
        accessPC:     Int,
        receiver:     AccessReceiver,
        baseMatchers: Set[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit =
        failure(accessPC, receiver, None, baseMatchers)

    override protected def failure(
        accessPC:     Int,
        receiver:     AccessReceiver,
        param:        Option[AccessParameter],
        baseMatchers: Set[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit = {
        if (HighSoundnessMode)
            addFieldRead(
                state.callContext, accessPC,
                _ => receiver,
                baseMatchers + AllFieldsMatcher,
            )
        else
            indirectFieldAccesses.addIncompleteAccessSite(accessPC)
    }
}

class FieldSetAnalysis private[analyses] (
        final val project:       SomeProject,
        final val apiMethodName: String,
        final val fieldType:     Option[BaseType] = None
) extends ReflectionAnalysis with TypeConsumerAnalysis with FieldInstanceBasedReflectiveFieldAccessAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Field,
        "",
        ObjectType.Field,
        apiMethodName,
        MethodDescriptor.apply(ArraySeq(ObjectType.Object, fieldType.getOrElse(ObjectType.Object)), VoidType)
    )

    override def processNewCaller(
        calleeContext:   ContextType,
        callerContext:   ContextType,
        accessPC:        Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
        implicit val state: ReflectionState[ContextType] = new ReflectionState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleFieldWrite(callerContext, accessPC, receiverOption.get.asVar, params, tac.stmts)
        } else {
            indirectFieldAccesses.addIncompleteAccessSite(accessPC)
        }

        returnResult(receiverOption.map(_.asVar).orNull)
    }

    private def returnResult(fieldVar: V)(
        implicit
        state:                 ReflectionState[ContextType],
        indirectFieldAccesses: IndirectFieldAccesses
    ): ProperPropertyComputationResult = {
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, continuation(fieldVar, state)),
                indirectFieldAccesses.partialResults(state.callContext)
            )
        else
            Results(indirectFieldAccesses.partialResults(state.callContext))
    }

    private case class FieldDepender(pc: Int, receiver: AccessReceiver, param: AccessParameter, matchers: Set[FieldMatcher])
    private case class ArrayDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            param:            AccessParameter,
            matchers:         Set[FieldMatcher],
            arrayVar:         V,
            callerStatements: Array[Stmt[V]]
    )
    private case class NameDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            param:            AccessParameter,
            matchers:         Set[FieldMatcher],
            nameVar:          V,
            callerStatements: Array[Stmt[V]],
            classVar:         V,
            callerContext:    ContextType
    )
    private case class ClassDepender(
            pc:               Int,
            receiver:         AccessReceiver,
            param:            AccessParameter,
            matchers:         Set[FieldMatcher],
            classVar:         V,
            callerStatements: Array[Stmt[V]]
    )

    protected def constructArrayDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], arrayVar: V, stmts: Array[Stmt[V]]
    ): Entity = ArrayDepender(accessPC, receiver, param.get, matchers, arrayVar, stmts)

    protected def constructNameDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher],
        nameVar: V, stmts: Array[Stmt[V]], classVar: V, context: ContextType
    ): Entity = NameDepender(accessPC, receiver, param.get, matchers, nameVar, stmts, classVar, context)

    protected def constructClassDepender(
        accessPC: Int, receiver: AccessReceiver, param: Option[AccessParameter], matchers: Set[FieldMatcher], classVar: V, stmts: Array[Stmt[V]]
    ): Entity = ClassDepender(accessPC, receiver, param.get, matchers, classVar, stmts)

    private def continuation(fieldVar: V, state: ReflectionState[ContextType])(eps: SomeEPS): ProperPropertyComputationResult = {
        implicit val indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
        implicit val _state: ReflectionState[ContextType] = state

        AllocationsUtil.continuationForAllocation[FieldDepender, ContextType](
            eps, state.callContext, _ => (fieldVar, state.tac.stmts),
            _.isInstanceOf[FieldDepender], data => failure(data.pc, data.matchers, data.receiver, data.param)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetField(
                    allocationContext, data.pc, allocationIndex, data.receiver, Some(data.param), data.matchers, stmts
                )
                addFieldWrite(state.callContext, data.pc, _ => data.receiver, _ => data.param, allMatchers)
            }

        AllocationsUtil.continuationForAllocation[NameDepender, ContextType](
            eps, state.callContext, data => (data.nameVar, data.callerStatements),
            _.isInstanceOf[NameDepender],
            data => failure(data.pc, data.matchers, data.receiver, data.param)
        ) { (data, _, allocationIndex, stmts) =>
                val name = StringUtil.getString(allocationIndex, stmts)

                val nameMatcher = retrieveSuitableMatcher[Set[String]](
                    name.map(Set(_)),
                    data.pc,
                    v => new NameBasedFieldMatcher(v)
                )

                if (nameMatcher ne NoFieldsMatcher) {
                    val matchers = data.matchers + nameMatcher
                    val allMatchers = matchers +
                        MatcherUtil.retrieveClassBasedFieldMatcher(
                            data.callerContext,
                            data.classVar,
                            ClassDepender(data.pc, data.receiver, data.param, matchers, data.classVar, data.callerStatements),
                            data.pc,
                            stmts,
                            project,
                            () => failure(data.pc, data.matchers, data.receiver, data.param),
                            onlyFieldsExactlyInClass = !data.matchers.contains(PublicFieldMatcher)
                        )

                    addFieldWrite(state.callContext, data.pc, _ => data.receiver, _ => data.param, allMatchers)
                }
            }

        AllocationsUtil.continuationForAllocation[ClassDepender, ContextType](
            eps, state.callContext, data => (data.classVar, data.callerStatements),
            _.isInstanceOf[ClassDepender], data => failure(data.pc, data.matchers, data.receiver, data.param)
        ) { (data, allocationContext, allocationIndex, stmts) =>
                val classes = TypesUtil.getPossibleClasses(
                    allocationContext, allocationIndex, data,
                    stmts, project, () => failure(data.pc, data.matchers, data.receiver, data.param),
                    onlyObjectTypes = false
                )

                val matchers = data.matchers +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        Some(classes.map {
                            tpe => if (tpe.isObjectType) tpe.asObjectType else ObjectType.Object
                        }),
                        data.pc,
                        v => new ClassBasedFieldMatcher(v, !data.matchers.contains(PublicFieldMatcher))
                    )

                addFieldWrite(state.callContext, data.pc, _ => data.receiver, _ => data.param, matchers)
            }

        AllocationsUtil.continuationForAllocation[(ClassDepender, V, Array[Stmt[V]]), ContextType](
            eps, state.callContext, data => (data._2, data._3),
            _.isInstanceOf[(_, _)], data => failure(data._1.pc, data._1.matchers, data._1.receiver, data._1.param)
        ) { (data, _, allocationIndex, stmts) =>
                val classOpt = TypesUtil.getPossibleForNameClass(
                    allocationIndex, stmts, project, onlyObjectTypes = false
                )

                val matchers = data._1.matchers +
                    retrieveSuitableMatcher[Set[ObjectType]](
                        classOpt.map(Set(_)),
                        data._1.pc,
                        v => new ClassBasedFieldMatcher(v, !data._1.matchers.contains(PublicFieldMatcher))
                    )

                addFieldWrite(state.callContext, data._1.pc, _ => data._1.receiver, _ => data._1.param, matchers)
            }

        if (eps.isFinal) state.removeDependee(eps.toEPK)
        else state.updateDependency(eps)

        returnResult(fieldVar)
    }

    private[this] def handleFieldWrite(
        callContext:        ContextType,
        accessPC:           Int,
        fieldVar:           V,
        fieldSetParameters: Seq[Option[Expr[V]]],
        stmts:              Array[Stmt[V]]
    )(implicit state: ReflectionState[ContextType], indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        val (fieldSetReceiver, fieldSetActualParameter) = if (fieldSetParameters.size == 2) {
            (
                fieldSetParameters.head.map(_.asVar),
                fieldSetParameters(1).map(_.asVar)
            )
        } else (None, None)

        val baseMatchers = Set(
            MatcherUtil.retrieveSuitableMatcher[V](
                fieldSetReceiver,
                accessPC,
                v => new ActualReceiverBasedFieldMatcher(v.value.asReferenceValue)
            ),
            MatcherUtil.retrieveSuitableMatcher[V](
                fieldSetActualParameter,
                accessPC,
                new ActualParameterBasedFieldMatcher(_)
            ),
            MatcherUtil.retrieveSuitableNonEssentialMatcher[BaseType](
                fieldType,
                new BaseTypeBasedFieldMatcher(_)
            )
        )

        val persistentReceiver = fieldSetReceiver.flatMap(persistentUVar(_)(stmts))
        val persistentActualParam = fieldSetActualParameter.flatMap(persistentUVar(_)(stmts))
        val depender = FieldDepender(accessPC, persistentReceiver, persistentActualParam, baseMatchers)

        AllocationsUtil.handleAllocations(
            fieldVar, callContext, depender, state.tac.stmts, _ eq ObjectType.Field,
            () => failure(accessPC, baseMatchers, persistentReceiver, persistentActualParam)
        ) { (allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetField(
                    allocationContext, accessPC, allocationIndex,
                    persistentReceiver, Some(persistentActualParam),
                    baseMatchers, stmts
                )
                addFieldWrite(
                    callContext, accessPC,
                    _ => persistentReceiver,
                    _ => persistentActualParam,
                    allMatchers
                )
            }
    }

    private def failure(
        accessPC:     Int,
        baseMatchers: Set[FieldMatcher],
        receiver:     AccessReceiver,
        param:        AccessParameter
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit =
        failure(accessPC, receiver, Some(param), baseMatchers)

    override protected def failure(
        accessPC:     Int,
        receiver:     AccessReceiver,
        param:        Option[AccessParameter],
        baseMatchers: Set[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit = {
        if (HighSoundnessMode)
            addFieldWrite(
                state.callContext, accessPC,
                _ => receiver, _ => param.get,
                baseMatchers + AllFieldsMatcher,
            )
        else
            indirectFieldAccesses.addIncompleteAccessSite(accessPC)
    }
}

object ReflectionRelatedFieldAccessesAnalysis {

    final val ConfigKey = {
        "org.opalj.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysis.highSoundness"
    }
}

/**
 * Records field accesses made through various reflective tools.
 *
 * @author Maximilian Rüsch
 */
class ReflectionRelatedFieldAccessesAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    def process(): PropertyComputationResult = {
        val analyses = List(
            /*
             * Field.get | Field.get[_ <: BaseType]
             */
            new FieldGetAnalysis(project, "get"),
            new FieldGetAnalysis(project, "getBoolean", Some(BooleanType)),
            new FieldGetAnalysis(project, "getByte", Some(ByteType)),
            new FieldGetAnalysis(project, "getChar", Some(CharType)),
            new FieldGetAnalysis(project, "getDouble", Some(DoubleType)),
            new FieldGetAnalysis(project, "getFloat", Some(FloatType)),
            new FieldGetAnalysis(project, "getInt", Some(IntegerType)),
            new FieldGetAnalysis(project, "getLong", Some(LongType)),
            new FieldGetAnalysis(project, "getShort", Some(ShortType)),

            /*
             * Field.set | Field.set[_ <: BaseType]
             */
            new FieldSetAnalysis(project, "set"),
            new FieldSetAnalysis(project, "setBoolean", Some(BooleanType)),
            new FieldSetAnalysis(project, "setByte", Some(ByteType)),
            new FieldSetAnalysis(project, "setChar", Some(CharType)),
            new FieldSetAnalysis(project, "setDouble", Some(DoubleType)),
            new FieldSetAnalysis(project, "setFloat", Some(FloatType)),
            new FieldSetAnalysis(project, "setInt", Some(IntegerType)),
            new FieldSetAnalysis(project, "setLong", Some(LongType)),
            new FieldSetAnalysis(project, "setShort", Some(ShortType)),

        /*
             * TODO MethodHandle.findSetter etc. (lookup again)
             */
        )

        Results(analyses.map(_.registerAPIMethod()))
    }
}

object ReflectionRelatedFieldAccessesAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, ProjectIndexKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )
    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = p.get(TypeIteratorKey).usedPropertyKinds

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReflectionRelatedFieldAccessesAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(_ => analysis.process())
        analysis
    }
}
