/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.FieldType
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
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1ArrayAccessInterpreter
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

    override type State = L1ComputationState

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

    /**
     * Returns the current interim result for the given state. If required, custom lower and upper
     * bounds can be used for the interim result.
     */
    private def getInterimResult(state: State): InterimResult[StringConstancyProperty] = InterimResult(
        state.entity,
        computeNewLowerBound(state),
        computeNewUpperBound(state),
        state.dependees.toSet,
        continuation(state)
    )

    private def computeNewUpperBound(state: State): StringConstancyProperty = {
        if (state.computedLeanPath != null) {
            StringConstancyProperty(new PathTransformer(state.interimIHandler).pathToStringTree(
                state.computedLeanPath,
                state.interimFpe2sci
            )(state).reduce(true))
        } else {
            StringConstancyProperty.lb
        }
    }

    private def computeNewLowerBound(state: State): StringConstancyProperty = StringConstancyProperty.lb

    def analyze(data: SContext): ProperPropertyComputationResult = {
        val dm = declaredMethods(data._2)
        val state = L1ComputationState(dm, data)

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
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    override protected[string_analysis] def determinePossibleStrings(state: State): ProperPropertyComputationResult = {
        val puVar = state.entity._1
        val uVar = puVar.toValueOriginForm(state.tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        if (state.tac == null || state.callees == null) {
            return getInterimResult(state)
        }

        val stmts = state.tac.stmts

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar, state.tac)
        }

        if (state.iHandler == null) {
            state.iHandler = L1InterpretationHandler(
                state.tac,
                ps,
                project,
                declaredFields,
                fieldAccessInformation,
                state,
                contextProvider
            )
            val interimState = state.copy()
            interimState.tac = state.tac
            interimState.computedLeanPath = state.computedLeanPath
            interimState.callees = state.callees
            interimState.callers = state.callers
            interimState.params = state.params
            state.interimIHandler = L1InterpretationHandler(
                state.tac,
                ps,
                project,
                declaredFields,
                fieldAccessInformation,
                interimState,
                contextProvider
            )
        }

        var requiresCallersInfo = false
        if (state.params.isEmpty) {
            state.params = L1StringAnalysis.getParams(state.entity)
        }
        if (state.params.isEmpty) {
            // In case a parameter is required for approximating a string, retrieve callers information
            // (but only once and only if the expressions is not a local string)
            val hasCallersOrParamInfo = state.callers == null && state.params.isEmpty
            requiresCallersInfo = if (defSites.exists(_ < 0)) {
                if (InterpretationHandler.isStringConstExpression(uVar)) {
                    hasCallersOrParamInfo
                } else if (L1StringAnalysis.isSupportedPrimitiveNumberType(uVar)) {
                    val numType = uVar.value.asPrimitiveValue.primitiveType.toJava
                    val sci = L1StringAnalysis.getDynamicStringInformationForNumberType(numType)
                    return Result(state.entity, StringConstancyProperty(sci))
                } else {
                    // StringBuilders as parameters are currently not evaluated
                    return Result(state.entity, StringConstancyProperty.lb)
                }
            } else {
                val call = stmts(defSites.head).asAssignment.expr
                if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
                    val (_, hasInitDefSites) = computeLeanPathForStringBuilder(uVar, state.tac)
                    if (!hasInitDefSites) {
                        return Result(state.entity, StringConstancyProperty.lb)
                    }
                    val hasSupportedParamType = state.entity._2.parameterTypes.exists {
                        L1StringAnalysis.isSupportedType
                    }
                    if (hasSupportedParamType) {
                        hasFormalParamUsageAlongPath(state.computedLeanPath, state.tac.stmts)
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
            val r = state.iHandler.processDefSite(defSites.head, state.params.toList.map(_.toList))(state)
            return Result(state.entity, StringConstancyProperty(r.asFinal.p.stringConstancyInformation))
        }

        val call = stmts(defSites.head).asAssignment.expr
        var attemptFinalResultComputation = false
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(state.computedLeanPath, stmts, puVar)(state)
            if (dependentVars.nonEmpty) {
                dependentVars.keys.foreach { nextVar =>
                    dependentVars.foreach { case (k, v) => state.appendToVar2IndexMapping(k, v) }
                    val ep = propertyStore((nextVar, state.entity._2), StringConstancyProperty.key)
                    ep match {
                        case FinalEP(e, p) => return processFinalP(state, e, p)
                        case _             => state.dependees = ep :: state.dependees
                    }
                }
            } else {
                attemptFinalResultComputation = true
            }
        } else {
            // If not a call to String{Builder, Buffer}.toString, then we deal with pure strings
            attemptFinalResultComputation = true
        }

        var sci = StringConstancyInformation.lb
        if (attemptFinalResultComputation
            && state.dependees.isEmpty
            && computeResultsForPath(state.computedLeanPath, state)
        ) {
            // Check whether we deal with the empty string; it requires special treatment as the
            // PathTransformer#pathToStringTree would not handle it correctly (as
            // PathTransformer#pathToStringTree is involved in a mutual recursion)
            val isEmptyString = if (state.computedLeanPath.elements.length == 1) {
                state.computedLeanPath.elements.head match {
                    case FlatPathElement(i) =>
                        state.fpe2sci.contains(i) && state.fpe2sci(i).length == 1 &&
                            state.fpe2sci(i).head == StringConstancyInformation.getNeutralElement
                    case _ => false
                }
            } else false

            sci = if (isEmptyString) {
                StringConstancyInformation.getNeutralElement
            } else {
                new PathTransformer(state.iHandler).pathToStringTree(
                    state.computedLeanPath,
                    state.fpe2sci
                )(state).reduce(true)
            }
        }

        if (state.dependees.nonEmpty) {
            getInterimResult(state)
        } else {
            L1StringAnalysis.unregisterParams(state.entity)
            Result(state.entity, StringConstancyProperty(sci))
        }
    }

    /**
     * Continuation function for this analysis.
     *
     * @param state The current computation state. Within this continuation, dependees of the state
     *              might be updated. Furthermore, methods processing this continuation might alter
     *              the state.
     * @return Returns a final result if (already) available. Otherwise, an intermediate result will
     *         be returned.
     */
    override protected def continuation(
        state: L1ComputationState
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
        case FlatPathElement(index) =>
            if (!state.fpe2sci.contains(index)) {
                iHandler.finalizeDefSite(index, state)
            }
        case npe: NestedPathElement =>
            finalizePreparations(Path(npe.element.toList), state, iHandler)
        case _ =>
    }

    /**
     * This method takes a computation state, `state` as well as a TAC provider, `tacProvider`, and
     * determines the interpretations of all parameters of the method under analysis. These
     * interpretations are registered using [[L1StringAnalysis.registerParams]].
     * The return value of this function indicates whether a the parameter evaluation is done
     * (`true`) or not yet (`false`).
     */
    private def registerParams(state: L1ComputationState): Boolean = {
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
                        if (L1StringAnalysis.isSupportedType(p.asVar)) {
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
            L1StringAnalysis.registerParams(state.entity, state.params)
        }
        !hasIntermediateResult
    }

    /**
     * This function traverses the given path, computes all string values along the path and stores
     * these information in the given state.
     *
     * @param p     The path to traverse.
     * @param state The current state of the computation. This function will alter
     *              [[L1ComputationState.fpe2sci]].
     * @return Returns `true` if all values computed for the path are final results.
     */
    private def computeResultsForPath(
        p:     Path,
        state: L1ComputationState
    ): Boolean = {
        var hasFinalResult = true

        p.elements.foreach {
            case FlatPathElement(index) =>
                if (!state.fpe2sci.contains(index)) {
                    val eOptP = state.iHandler.processDefSite(index, state.params.toList.map(_.toSeq))(state)
                    if (eOptP.isFinal) {
                        state.appendToFpe2Sci(index, eOptP.asFinal.p.stringConstancyInformation, reset = true)
                    } else {
                        hasFinalResult = false
                    }
                }
            case npe: NestedPathElement =>
                val subFinalResult = computeResultsForPath(
                    Path(npe.element.toList),
                    state
                )
                hasFinalResult = hasFinalResult && subFinalResult
            case _ =>
        }

        hasFinalResult
    }

    private def hasFormalParamUsageAlongPath(path: Path, stmts: Array[Stmt[V]]): Boolean = {
        def hasExprFormalParamUsage(expr: Expr[V]): Boolean = expr match {
            case al: ArrayLoad[V]    => L1ArrayAccessInterpreter.getStoreAndLoadDefSites(al, stmts).exists(_ < 0)
            case duVar: V            => duVar.definedBy.exists(_ < 0)
            case fc: FunctionCall[V] => fc.params.exists(hasExprFormalParamUsage)
            case mc: MethodCall[V]   => mc.params.exists(hasExprFormalParamUsage)
            case be: BinaryExpr[V]   => hasExprFormalParamUsage(be.left) || hasExprFormalParamUsage(be.right)
            case _                   => false
        }

        path.elements.exists {
            case FlatPathElement(index) => stmts(index) match {
                    case Assignment(_, _, expr) => hasExprFormalParamUsage(expr)
                    case ExprStmt(_, expr)      => hasExprFormalParamUsage(expr)
                    case _                      => false
                }
            case NestedPathElement(subPath, _) => hasFormalParamUsageAlongPath(Path(subPath.toList), stmts)
            case _                             => false
        }
    }
}

object L1StringAnalysis {

    final val FieldWriteThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.fieldWriteThreshold"
    }

    private final val CallersThresholdConfigKey = {
        "org.opalj.fpcf.analyses.string_analysis.l1.L1StringAnalysis.callersThreshold"
    }

    /**
     * Maps entities to a list of lists of parameters. As currently this analysis works context-
     * insensitive, we have a list of lists to capture all parameters of all potential method /
     * function calls.
     */
    private val paramInfos = mutable.Map[Entity, ListBuffer[ListBuffer[StringConstancyInformation]]]()

    def registerParams(e: Entity, scis: ListBuffer[ListBuffer[StringConstancyInformation]]): Unit = {
        if (!paramInfos.contains(e)) {
            paramInfos(e) = scis
        } else {
            paramInfos(e).appendAll(scis)
        }
    }

    def unregisterParams(e: Entity): Unit = paramInfos.remove(e)

    def getParams(e: Entity): ListBuffer[ListBuffer[StringConstancyInformation]] =
        if (paramInfos.contains(e)) {
            paramInfos(e)
        } else {
            ListBuffer()
        }

    /**
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    def isSupportedPrimitiveNumberType(v: V): Boolean =
        v.value.isPrimitiveValue && isSupportedPrimitiveNumberType(v.value.asPrimitiveValue.primitiveType.toJava)

    /**
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    def isSupportedPrimitiveNumberType(typeName: String): Boolean =
        typeName == "short" || typeName == "int" || typeName == "float" || typeName == "double"

    /**
     * Checks whether a given type, identified by its string representation, is supported by the
     * string analysis. That means, if this function returns `true`, a value, which is of type
     * `typeName` may be approximated by the string analysis better than just the lower bound.
     *
     * @param typeName The name of the type to check. May either be the name of a primitive type or
     *                 a fully-qualified class name (dot-separated).
     * @return Returns `true`, if `typeName` is an element in [char, short, int, float, double,
     *         java.lang.String] and `false` otherwise.
     */
    def isSupportedType(typeName: String): Boolean =
        typeName == "char" || isSupportedPrimitiveNumberType(typeName) ||
            typeName == "java.lang.String" || typeName == "java.lang.String[]"

    /**
     * Determines whether a given element is supported by the string analysis.
     *
     * @param v The element to check.
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[L1StringAnalysis.isSupportedType(String)]].
     */
    def isSupportedType(v: V): Boolean =
        if (v.value.isPrimitiveValue) {
            isSupportedType(v.value.asPrimitiveValue.primitiveType.toJava)
        } else {
            try {
                isSupportedType(v.value.verificationTypeInfo.asObjectVariableInfo.clazz.toJava)
            } catch {
                case _: Exception => false
            }
        }

    /**
     * Determines whether a given [[FieldType]] element is supported by the string analysis.
     *
     * @param fieldType The element to check.
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[L1StringAnalysis.isSupportedType(String)]].
     */
    def isSupportedType(fieldType: FieldType): Boolean = isSupportedType(fieldType.toJava)

    /**
     * Takes the name of a primitive number type - supported types are short, int, float, double -
     * and returns the dynamic [[StringConstancyInformation]] for that type. In case an unsupported
     * type is given [[StringConstancyInformation.UnknownWordSymbol]] is returned as possible
     * strings.
     */
    def getDynamicStringInformationForNumberType(
        numberType: String
    ): StringConstancyInformation = {
        val possibleStrings = numberType match {
            case "short" | "int"    => StringConstancyInformation.IntValue
            case "float" | "double" => StringConstancyInformation.FloatValue
            case _                  => StringConstancyInformation.UnknownWordSymbol
        }
        StringConstancyInformation(StringConstancyLevel.DYNAMIC, possibleStrings = possibleStrings)
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
