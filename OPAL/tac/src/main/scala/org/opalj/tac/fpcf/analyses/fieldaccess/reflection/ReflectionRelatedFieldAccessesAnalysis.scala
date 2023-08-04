/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedFields
import org.opalj.br.DefinedFieldsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.tac.fpcf.analyses.cg.persistentUVar
import org.opalj.br.Field
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.IndirectFieldAccesses
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.fpcf.EOptionP
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
            "field access reflection analysis uses " + (if (activated) "high soundness mode" else "standard mode")
        ))
        activated
    }

    def addFieldRead(
        callContext:    ContextType,
        callPC:         Int,
        actualReceiver: Field => Option[(ValueInformation, IntTrieSet)],
        matchers:       Iterable[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        FieldMatching.getPossibleFields(matchers.toSeq).foreach { f =>
            indirectFieldAccesses.addFieldRead(
                callContext,
                callPC,
                definedFields(f),
                actualReceiver(f)
            )
        }
    }
}

class FieldGetAnalysis private[analyses] ( final val project: SomeProject)
    extends ReflectionAnalysis with TypeConsumerAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectType.Field,
        "",
        ObjectType.Field,
        "get",
        MethodDescriptor.apply(ObjectType.Object, ObjectType.Object)
    )

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
        implicit val indirectFieldAccesses: IndirectFieldAccesses = new IndirectFieldAccesses()
        implicit val state: ReflectionState[ContextType] = new ReflectionState[ContextType](
            callerContext, FinalEP(callerContext.method.definedMethod, TheTACAI(tac))
        )

        if (receiverOption.isDefined) {
            handleFieldRead(callerContext, callPC, receiverOption.get.asVar, params.head, tac.stmts)
        } else {
            failure(callPC, None, Set.empty)
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

    private case class FieldDepender(pc: Int, receiver: Option[(ValueInformation, IntTrieSet)], matchers: Set[FieldMatcher])
    private case class NameDepender(
            pc:               Int,
            receiver:         Option[(ValueInformation, IntTrieSet)],
            matchers:         Set[FieldMatcher],
            nameVar:          V,
            callerStatements: Array[Stmt[V]],
            classVar:         V,
            callerContext:    ContextType
    )
    private case class ClassDepender(
            pc:               Int,
            receiver:         Option[(ValueInformation, IntTrieSet)],
            matchers:         Set[FieldMatcher],
            classVar:         V,
            callerStatements: Array[Stmt[V]]
    )

    // TODO how to test functionality of this?
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
                        v => new ClassBasedMethodMatcher(v, !data.matchers.contains(PublicFieldMatcher)) // TODO is public field matcher used to indicate "getField"?????
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
                        v => new ClassBasedMethodMatcher(v, !data._1.matchers.contains(PublicFieldMatcher)) // TODO is public field matcher used to indicate "getField"?????
                    )

                addFieldRead(state.callContext, data._1.pc, _ => data._1.receiver, matchers)
            }

        if (eps.isFinal) state.removeDependee(eps.toEPK)
        else state.updateDependency(eps)

        returnResult(fieldVar)
    }

    private[this] def handleFieldRead(
        callContext:       ContextType,
        callPC:            Int,
        fieldVar:          V,
        fieldGetParameter: Option[Expr[V]],
        stmts:             Array[Stmt[V]]
    )(implicit state: ReflectionState[ContextType], indirectFieldAccesses: IndirectFieldAccesses): Unit = {
        val fieldGetReceiver: Option[V] = fieldGetParameter.map(_.asVar)

        val baseMatchers = Set(
            MatcherUtil.retrieveSuitableMatcher[V](
                fieldGetReceiver,
                callPC,
                v => new ActualReceiverBasedFieldMatcher(v.value.asReferenceValue)
            )
        )

        val persistentReceiver = fieldGetReceiver.flatMap(r => persistentUVar(r)(stmts))
        val depender = FieldDepender(callPC, persistentReceiver, baseMatchers)

        AllocationsUtil.handleAllocations(
            fieldVar, callContext, depender, state.tac.stmts, _ eq ObjectType.Field,
            () => failure(callPC, persistentReceiver, baseMatchers)
        ) { (allocationContext, allocationIndex, stmts) =>
                val allMatchers = handleGetField(
                    allocationContext, callPC, allocationIndex,
                    persistentReceiver,
                    baseMatchers, stmts
                )
                addFieldRead(
                    callContext, callPC,
                    _ => persistentReceiver,
                    allMatchers
                )
            }
    }

    private[this] def handleGetField(
        context:        ContextType,
        callPC:         Int,
        fieldDefSite:   Int,
        actualReceiver: Option[(ValueInformation, IntTrieSet)],
        baseMatchers:   Set[FieldMatcher],
        stmts:          Array[Stmt[V]]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Set[FieldMatcher] = {
        var matchers = baseMatchers
        stmts(fieldDefSite).asAssignment.expr match {
            case call @ VirtualFunctionCall(_, ObjectType.Class, _, "getDeclaredField" | "getField", _, receiver, params) =>

                // matchers += MatcherUtil.retrieveTypeBasedFieldMatcher(params(1), callPC, stmts) TODO what with this

                val isGetField = call.name == "getField"
                if (isGetField)
                    matchers += PublicFieldMatcher

                if (!matchers.contains(NoFieldsMatcher))
                    matchers += MatcherUtil.retrieveNameBasedFieldMatcher(
                        context,
                        params.head.asVar,
                        NameDepender(
                            callPC, actualReceiver, matchers,
                            params.head.asVar, stmts, receiver.asVar,
                            context
                        ),
                        callPC,
                        stmts,
                        () => failure(callPC, actualReceiver, matchers)
                    )

                if (!matchers.contains(NoFieldsMatcher)) {
                    // THIS MATCHER FAILS IF ANY POSSIBLE CLASS IS NOT FINAL TODO wtf
                    matchers += MatcherUtil.retrieveClassBasedFieldMatcher(
                        context,
                        receiver.asVar,
                        ClassDepender(callPC, actualReceiver, matchers, receiver.asVar, stmts),
                        callPC,
                        stmts,
                        project,
                        () => failure(callPC, actualReceiver, matchers),
                        onlyFieldsExactlyInClass = !isGetField
                    )
                }

            /*case ArrayLoad(_, _, arrayRef) =>*/
            // TODO here we can handle getFields

            case _ =>
                if (HighSoundnessMode) {
                    matchers += AllFieldsMatcher
                } else {
                    indirectFieldAccesses.addIncompleteAccessSite(callPC)
                    matchers += NoFieldsMatcher
                }
        }

        matchers
    }

    private[this] def failure(
        callPC:       Int,
        receiver:     Option[(ValueInformation, IntTrieSet)],
        baseMatchers: Set[FieldMatcher]
    )(implicit indirectFieldAccesses: IndirectFieldAccesses, state: ReflectionState[ContextType]): Unit = {
        if (HighSoundnessMode) {
            addFieldRead(
                state.callContext, callPC,
                _ => receiver,
                baseMatchers + AllFieldsMatcher,
            )
        } else {
            indirectFieldAccesses.addIncompleteAccessSite(callPC)
        }
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
             * TODO Field.get[*]
             */
            new FieldGetAnalysis(project),

            /*
             * TODO Field.set[*]
             */

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
