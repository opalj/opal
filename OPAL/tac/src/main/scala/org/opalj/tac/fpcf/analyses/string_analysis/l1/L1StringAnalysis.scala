/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import scala.collection.mutable.ListBuffer

import org.opalj.br.DeclaredMethod
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0ArrayAccessInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.properties.TACAI

/**
 * InterproceduralStringAnalysis processes a read operation of a string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * In comparison to [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]], this version tries to resolve
 * method calls that are involved in a string construction as far as possible.
 * <p>
 * The main difference in the intra- and interprocedural implementation is the following (see the
 * description of [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]] for a general overview):
 * This analysis can only start to transform the computed lean paths into a string tree (again using a
 * [[PathTransformer]]) after all relevant string values (determined by the [[L1InterpretationHandler]])
 * have been figured out. As the [[PropertyStore]] is used for recursively starting this analysis
 * to determine possible strings of called method and functions, the path transformation can take
 * place after all results for sub-expressions are available. Thus, the interprocedural
 * interpretation handler cannot determine final results, e.g., for the array interpreter or static
 * function call interpreter. This analysis handles this circumstance by first collecting all
 * information for all definition sites. Only when these are available, further information, e.g.,
 * for the final results of arrays or static function calls, are derived. Finally, after all
 * these information are ready as well, the path transformation takes place by only looking up what
 * string expression corresponds to which definition sites (remember, at this point, for all
 * definition sites all possible string values are known, thus look-ups are enough and no further
 * interpretation is required).
 *
 * @author Patrick Mell
 */
class L1StringAnalysis(val project: SomeProject) extends StringAnalysis {

    protected[l1] case class CState(
        override val dm:            DeclaredMethod,
        override val entity:        (SEntity, Method),
        override val methodContext: Context
    ) extends L1ComputationState[CState]

    override type State = CState

    /**
     * To analyze an expression within a method ''m'', callers information might be necessary, e.g.,
     * to know with which arguments ''m'' is called. [[callersThreshold]] determines the threshold
     * up to which number of callers parameter information are gathered. For "number of callers
     * greater than [[callersThreshold]]", parameters are approximated with the lower bound.
     */
    private val callersThreshold = {
        val threshold =
            try {
                project.config.getInt(L1StringAnalysis.CallersThresholdConfigKey)
            } catch {
                case t: Throwable =>
                    logOnce(Error(
                        "analysis configuration - l1 string analysis",
                        s"couldn't read: ${L1StringAnalysis.CallersThresholdConfigKey}",
                        t
                    ))
                    10
            }

        logOnce(Info(
            "analysis configuration - l1 string analysis",
            "l1 string analysis uses a callers threshold of " + threshold
        ))
        threshold
    }

    protected implicit val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)
    protected implicit val fieldAccessInformation: FieldAccessInformation = project.get(FieldAccessInformationKey)
    protected implicit val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def analyze(data: SContext): ProperPropertyComputationResult = {
        val dm = declaredMethods(data._2)
        // IMPROVE enable handling call string contexts here (build a chain, probably via SContext)
        val state = CState(dm, data, contextProvider.newContext(declaredMethods(data._2)))

        val tacaiEOptP = ps(data._2, TACAI.key)
        if (tacaiEOptP.hasUBP) {
            if (tacaiEOptP.ub.tac.isEmpty) {
                // No TAC available, e.g., because the method has no body
                return Result(state.entity, StringConstancyProperty.lb)
            } else {
                state.tac = tacaiEOptP.ub.tac.get
            }
        } else {
            state.dependees = tacaiEOptP :: state.dependees
        }

        val calleesEOptP = ps(dm, Callees.key)
        if (calleesEOptP.hasUBP) {
            state.callees = calleesEOptP.ub
            determinePossibleStrings(state)
        } else {
            state.dependees = calleesEOptP :: state.dependees
            getInterimResult(state)
        }
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[org.opalj.fpcf.InterimResult]] depending on whether other information needs to be computed first.
     */
    override protected[string_analysis] def determinePossibleStrings(implicit
        state: State
    ): ProperPropertyComputationResult = {
        val puVar = state.entity._1
        val uVar = puVar.toValueOriginForm(state.tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        if (state.tac == null || state.callees == null) {
            return getInterimResult(state)
        }

        val stmts = state.tac.stmts

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar)(state.tac)
        }

        if (state.iHandler == null) {
            state.iHandler =
                L1InterpretationHandler(declaredFields, fieldAccessInformation, project, ps, contextProvider)
            state.interimIHandler =
                L1InterpretationHandler(declaredFields, fieldAccessInformation, project, ps, contextProvider)
        }

        var requiresCallersInfo = false
        if (state.params.isEmpty) {
            state.params = StringAnalysis.getParams(state.entity)
        }
        if (state.params.isEmpty) {
            // In case a parameter is required for approximating a string, retrieve callers information
            // (but only once and only if the expressions is not a local string)
            val hasCallersOrParamInfo = state.callers == null && state.params.isEmpty
            requiresCallersInfo = if (defSites.exists(_ < 0)) {
                if (InterpretationHandler.isStringConstExpression(uVar)) {
                    hasCallersOrParamInfo
                } else if (StringAnalysis.isSupportedPrimitiveNumberType(uVar)) {
                    val numType = uVar.value.asPrimitiveValue.primitiveType.toJava
                    val sci = StringAnalysis.getDynamicStringInformationForNumberType(numType)
                    return Result(state.entity, StringConstancyProperty(sci))
                } else {
                    // StringBuilders as parameters are currently not evaluated
                    return Result(state.entity, StringConstancyProperty.lb)
                }
            } else {
                val call = stmts(defSites.head).asAssignment.expr
                if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
                    val leanPath = computeLeanPathForStringBuilder(uVar)(state.tac)
                    if (leanPath.isEmpty) {
                        return Result(state.entity, StringConstancyProperty.lb)
                    }
                    val hasSupportedParamType = state.entity._2.parameterTypes.exists {
                        StringAnalysis.isSupportedType
                    }
                    if (hasSupportedParamType) {
                        hasFormalParamUsageAlongPath(state.computedLeanPath)(state.tac)
                    } else {
                        !hasCallersOrParamInfo
                    }
                } else {
                    !hasCallersOrParamInfo
                }
            }
        }

        if (requiresCallersInfo) {
            val dm = declaredMethods(state.entity._2)
            val callersEOptP = ps(dm, Callers.key)
            if (callersEOptP.hasUBP) {
                state.callers = callersEOptP.ub
                if (!registerParams(state)) {
                    return getInterimResult(state)
                }
            } else {
                state.dependees = callersEOptP :: state.dependees
                return getInterimResult(state)
            }
        }

        if (state.parameterDependeesCount > 0) {
            return getInterimResult(state)
        } else {
            state.isSetupCompleted = true
        }

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val r = state.iHandler.processDefSite(defSites.head)(state)
            return Result(state.entity, StringConstancyProperty(r.asFinal.sci))
        }

        val call = stmts(defSites.head).asAssignment.expr
        var attemptFinalResultComputation = true
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            // Find DUVars that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath, puVar)(state)
            if (dependentVars.nonEmpty) {
                dependentVars.keys.foreach { nextVar =>
                    dependentVars.foreach { case (k, v) => state.appendToVar2IndexMapping(k, v) }
                    val ep = propertyStore((nextVar, state.entity._2), StringConstancyProperty.key)
                    ep match {
                        case FinalEP(e, p) =>
                            processFinalP(state, e, p)
                            // No more dependees => Return the result for this analysis run
                            if (state.dependees.isEmpty) {
                                return computeFinalResult(state)
                            } else {
                                return getInterimResult(state)
                            }
                        case _ =>
                            state.dependees = ep :: state.dependees
                            attemptFinalResultComputation = false
                    }
                }
            }
        }

        val sci =
            if (attemptFinalResultComputation
                && state.dependees.isEmpty
                && computeResultsForPath(state.computedLeanPath)(state)
            ) {
                new PathTransformer(state.iHandler)
                    .pathToStringTree(state.computedLeanPath, state.fpe2sci)
                    .reduce(true)
            } else {
                StringConstancyInformation.lb
            }

        if (state.dependees.nonEmpty) {
            getInterimResult(state)
        } else {
            StringAnalysis.unregisterParams(state.entity)
            Result(state.entity, StringConstancyProperty(sci))
        }
    }

    /**
     * Continuation function for this analysis.
     *
     * @param state The current computation state. Within this continuation, dependees of the state might be updated.
     *              Furthermore, methods processing this continuation might alter the state.
     * @return Returns a final result if (already) available. Otherwise, an intermediate result will be returned.
     */
    override protected def continuation(
        state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        state.dependees = state.dependees.filter(_.e != eps.e)

        eps match {
            case FinalP(callees: Callees) if eps.pk.equals(Callees.key) =>
                state.callees = callees
                if (state.dependees.isEmpty) {
                    determinePossibleStrings(state)
                } else {
                    getInterimResult(state)
                }
            case FinalP(callers: Callers) if eps.pk.equals(Callers.key) =>
                state.callers = callers
                if (state.dependees.isEmpty) {
                    registerParams(state)
                    determinePossibleStrings(state)
                } else {
                    getInterimResult(state)
                }
            case _ =>
                super.continuation(state)(eps)
        }
    }

    override protected[string_analysis] def finalizePreparations(
        path:     Path,
        state:    State,
        iHandler: InterpretationHandler[State]
    ): Unit = path.elements.foreach {
        case fpe: FlatPathElement =>
            if (!state.fpe2sci.contains(fpe.pc)) {
                iHandler.finalizeDefSite(valueOriginOfPC(fpe.pc, state.tac.pcToIndex).get)(state)
            }
        case npe: NestedPathElement =>
            finalizePreparations(Path(npe.element.toList), state, iHandler)
        case _ =>
    }

    /**
     * This method takes a computation `state`, and determines the interpretations of all parameters of the method under
     * analysis. These interpretations are registered using [[StringAnalysis.registerParams]]. The return value of this
     * function indicates whether the parameter evaluation is done (`true`) or not yet (`false`).
     */
    private def registerParams(state: State): Boolean = {
        val callers = state.callers.callers(state.dm)(contextProvider).iterator.toSeq
        if (callers.length > callersThreshold) {
            state.params.append(
                ListBuffer.from(state.entity._2.parameterTypes.map {
                    _: FieldType => StringConstancyInformation.lb
                })
            )
            return false
        }

        var hasIntermediateResult = false
        callers.zipWithIndex.foreach {
            case ((m, pc, _), methodIndex) =>
                val tac = propertyStore(m.definedMethod, TACAI.key).ub.tac.get
                val params = tac.stmts(tac.pcToIndex(pc)) match {
                    case Assignment(_, _, fc: FunctionCall[V]) => fc.params
                    case Assignment(_, _, mc: MethodCall[V])   => mc.params
                    case ExprStmt(_, fc: FunctionCall[V])      => fc.params
                    case ExprStmt(_, fc: MethodCall[V])        => fc.params
                    case mc: MethodCall[V]                     => mc.params
                    case _                                     => List()
                }
                params.zipWithIndex.foreach {
                    case (p, paramIndex) =>
                        // Add an element to the params list (we do it here because we know how many
                        // parameters there are)
                        if (state.params.length <= methodIndex) {
                            state.params.append(ListBuffer.from(params.indices.map(_ =>
                                StringConstancyInformation.getNeutralElement
                            )))
                        }
                        // Recursively analyze supported types
                        if (StringAnalysis.isSupportedType(p.asVar)) {
                            val paramEntity = (p.asVar.toPersistentForm(state.tac.stmts), m.definedMethod)
                            val eps = propertyStore(paramEntity, StringConstancyProperty.key)
                            state.appendToVar2IndexMapping(paramEntity._1, paramIndex)
                            eps match {
                                case FinalP(r) =>
                                    state.params(methodIndex)(paramIndex) = r.stringConstancyInformation
                                case _ =>
                                    state.dependees = eps :: state.dependees
                                    hasIntermediateResult = true
                                    state.paramResultPositions(paramEntity) = (methodIndex, paramIndex)
                                    state.parameterDependeesCount += 1
                            }
                        } else {
                            state.params(methodIndex)(paramIndex) =
                                StringConstancyProperty.lb.stringConstancyInformation
                        }
                }
        }
        // If all parameters could already be determined, register them
        if (!hasIntermediateResult) {
            StringAnalysis.registerParams(state.entity, state.params)
        }
        !hasIntermediateResult
    }

    override protected def hasExprFormalParamUsage(expr: Expr[V])(implicit tac: TAC): Boolean = expr match {
        case al: ArrayLoad[V] => L0ArrayAccessInterpreter.getStoreAndLoadDefSites(al)(tac.stmts).exists(_ < 0)
        case _                => super.hasExprFormalParamUsage(expr)
    }
}

object L1StringAnalysis {

    private[l1] final val FieldWriteThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.fieldWriteThreshold"
    }

    private final val CallersThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.callersThreshold"
    }
}

sealed trait L1StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override final def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.lub(StringConstancyProperty)
    )

    override final type InitializationData = L1StringAnalysis
    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        new L1StringAnalysis(p)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

/**
 * Executor for the lazy analysis.
 */
object LazyL1StringAnalysis
    extends L1StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, analysis: InitializationData): FPCFAnalysis = {
        val analysis = new L1StringAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        FieldAccessInformationKey,
        ContextProviderKey
    )
}
