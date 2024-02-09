/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.FieldType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathType
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.WindowPathFinder
import org.opalj.tac.fpcf.properties.TACAI

/**
 * String Analysis trait defining some basic dependency handling.
 *
 * @author Maximilian Rüsch
 */
trait StringAnalysis extends FPCFAnalysis {

    type State <: ComputationState[State]

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Returns the current interim result for the given state. If required, custom lower and upper bounds can be used
     * for the interim result.
     */
    protected def getInterimResult(state: State): InterimResult[StringConstancyProperty] = InterimResult(
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

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    protected[string_analysis] def determinePossibleStrings(implicit state: State): ProperPropertyComputationResult

    /**
     * Continuation function for this analysis.
     *
     * @param state The current computation state. Within this continuation, dependees of the state
     *              might be updated. Furthermore, methods processing this continuation might alter
     *              the state.
     * @return Returns a final result if (already) available. Otherwise, an intermediate result will
     *         be returned.
     */
    protected[this] def continuation(state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
        state.dependees = state.dependees.filter(_.e != eps.e)

        eps match {
            case FinalP(tac: TACAI) if eps.pk.equals(TACAI.key) =>
                // Set the TAC only once (the TAC might be requested for other methods, so this
                // makes sure we do not overwrite the state's TAC)
                if (state.tac == null) {
                    state.tac = tac.tac.get
                }
                determinePossibleStrings(state)
            case FinalEP(entity, p: StringConstancyProperty) if eps.pk.equals(StringConstancyProperty.key) =>
                val e = entity.asInstanceOf[SContext]
                // For updating the interim state
                state.var2IndexMapping(eps.e.asInstanceOf[SContext]._1).foreach { i =>
                    state.appendToInterimFpe2Sci(i, p.stringConstancyInformation)
                }
                // If necessary, update the parameter information with which the
                // surrounding function / method of the entity was called with
                if (state.paramResultPositions.contains(e)) {
                    val pos = state.paramResultPositions(e)
                    state.params(pos._1)(pos._2) = p.stringConstancyInformation
                    state.paramResultPositions.remove(e)
                    state.parameterDependeesCount -= 1
                }

                // If necessary, update parameter information of function calls
                if (state.entity2Function.contains(e)) {
                    state.var2IndexMapping(e._1).foreach(state.appendToFpe2Sci(
                        _,
                        p.stringConstancyInformation
                    ))
                    // Update the state
                    state.entity2Function(e).foreach { f =>
                        val pos = state.nonFinalFunctionArgsPos(f)(e)
                        val finalEp = FinalEP(e, p)
                        state.nonFinalFunctionArgs(f)(pos._1)(pos._2)(pos._3) = finalEp
                        // Housekeeping
                        val index = state.entity2Function(e).indexOf(f)
                        state.entity2Function(e).remove(index)
                        if (state.entity2Function(e).isEmpty) {
                            state.entity2Function.remove(e)
                        }
                    }
                    // Continue only after all necessary function parameters are evaluated
                    if (state.entity2Function.nonEmpty) {
                        return getInterimResult(state)
                    } else {
                        // We could try to determine a final result before all function
                        // parameter information are available, however, this will
                        // definitely result in finding some intermediate result. Thus,
                        // defer this computations when we know that all necessary
                        // information are available
                        state.entity2Function.clear()
                        if (!computeResultsForPath(state.computedLeanPath)(state)) {
                            return determinePossibleStrings(state)
                        }
                    }
                }

                if (state.isSetupCompleted && state.parameterDependeesCount == 0) {
                    processFinalP(state, eps.e, p)
                    // No more dependees => Return the result for this analysis run
                    if (state.dependees.isEmpty) {
                        computeFinalResult(state)
                    } else {
                        getInterimResult(state)
                    }
                } else {
                    determinePossibleStrings(state)
                }
            case InterimLUBP(_: StringConstancyProperty, ub: StringConstancyProperty)
                if eps.pk.equals(StringConstancyProperty.key) =>
                state.dependees = eps :: state.dependees
                val puVar = eps.e.asInstanceOf[SContext]._1
                state.var2IndexMapping(puVar).foreach { i =>
                    state.appendToInterimFpe2Sci(
                        i,
                        ub.stringConstancyInformation,
                        Some(puVar)
                    )
                }
                getInterimResult(state)
            case _ =>
                state.dependees = eps :: state.dependees
                getInterimResult(state)

        }
    }

    protected[string_analysis] def finalizePreparations(
        path:     Path,
        state:    State,
        iHandler: InterpretationHandler[State]
    ): Unit = {}

    /**
     * computeFinalResult computes the final result of an analysis. This includes the computation
     * of instruction that could only be prepared (e.g., if an array load included a method call,
     * its final result is not yet ready, however, this function finalizes, e.g., that load).
     *
     * @param state The final computation state. For this state the following criteria must apply:
     *              For each [[FlatPathElement]], there must be a corresponding entry in
     *              `state.fpe2sci`. If this criteria is not met, a [[NullPointerException]] will
     *              be thrown (in this case there was some work to do left and this method should
     *              not have been called)!
     * @return Returns the final result.
     */
    protected def computeFinalResult(state: State): Result = {
        finalizePreparations(state.computedLeanPath, state, state.iHandler)
        val finalSci = new PathTransformer(state.iHandler).pathToStringTree(
            state.computedLeanPath,
            state.fpe2sci,
            resetExprHandler = false
        )(state).reduce(true)
        StringAnalysis.unregisterParams(state.entity)
        Result(state.entity, StringConstancyProperty(finalSci))
    }

    protected def processFinalP(
        state: State,
        e:     Entity,
        p:     StringConstancyProperty
    ): Unit = {
        // Add mapping information (which will be used for computing the final result)
        state.var2IndexMapping(e.asInstanceOf[SContext]._1).foreach {
            state.appendToFpe2Sci(_, p.stringConstancyInformation)
        }

        state.dependees = state.dependees.filter(_.e != e)
    }

    /**
     * This function traverses the given path, computes all string values along the path and stores
     * these information in the given state.
     *
     * @param p     The path to traverse.
     * @param state The current state of the computation. This function will alter [[ComputationState.fpe2sci]].
     * @return Returns `true` if all values computed for the path are final results.
     */
    protected def computeResultsForPath(p: Path)(implicit state: State): Boolean = {
        var hasFinalResult = true
        p.elements.foreach {
            case fpe: FlatPathElement =>
                if (!state.fpe2sci.contains(fpe.pc)) {
                    val eOptP = state.iHandler.processDefSite(valueOriginOfPC(fpe.pc, state.tac.pcToIndex).get)
                    if (eOptP.isFinal) {
                        state.appendToFpe2Sci(fpe.pc, eOptP.asFinal.p.stringConstancyInformation, reset = true)
                    } else {
                        hasFinalResult = false
                    }
                }
            case npe: NestedPathElement =>
                hasFinalResult = hasFinalResult && computeResultsForPath(Path(npe.element.toList))
            case _ =>
        }

        hasFinalResult
    }

    /**
     * Wrapper function for [[computeLeanPathForStringConst]] and [[computeLeanPathForStringBuilder]].
     */
    protected def computeLeanPath(value: V)(implicit tac: TAC): Path = {
        val defSites = value.definedBy.toArray.sorted
        if (defSites.head < 0) {
            computeLeanPathForStringConst(value)(tac.stmts)
        } else {
            val call = tac.stmts(defSites.head).asAssignment.expr
            if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
                computeLeanPathForStringBuilder(value).get
            } else {
                computeLeanPathForStringConst(value)(tac.stmts)
            }
        }
    }

    /**
     * This function computes the lean path for a [[V]] which is required to be a string expression.
     */
    protected def computeLeanPathForStringConst(value: V)(implicit stmts: Array[Stmt[V]]): Path = {
        val defSites = value.definedBy.toArray.sorted
        val element = if (defSites.length == 1) {
            FlatPathElement(defSites.head)
        } else {
            // Create alternative branches with intermediate None-Type nested path elements
            val children = defSites.map { ds => NestedPathElement(ListBuffer(FlatPathElement(ds)), None) }
            NestedPathElement(ListBuffer.from(children), Some(NestedPathType.CondWithAlternative))
        }
        Path(List(element))
    }

    /**
     * This function computes the lean path for a [[V]] which is required to stem from a
     * `String{Builder, Buffer}#toString()` call. For this, the `tac` of the method, in which `value` resides, is
     * required.
     *
     * This function then returns a pair of values: The first value is the computed lean path and the second value
     * indicates whether the String{Builder, Buffer} has initialization sites within the method stored in `tac`. If it
     * has no initialization sites, it returns `(null, false)` and otherwise `(computed lean path, true)`.
     */
    protected def computeLeanPathForStringBuilder(value: V)(implicit tac: TAC): Option[Path] = {
        val initDefSites = InterpretationHandler.findDefSiteOfInit(value, tac.stmts)
        if (initDefSites.isEmpty) {
            None
        } else {
            val path = WindowPathFinder(tac).findPaths(initDefSites, value.definedBy.toArray.max)
            val leanPath = path.makeLeanPath(value)
            Some(leanPath)
        }
    }

    protected def hasExprFormalParamUsage(expr: Expr[V])(implicit tac: TAC): Boolean = expr match {
        case duVar: V            => duVar.definedBy.exists(_ < 0)
        case fc: FunctionCall[V] => fc.params.exists(hasExprFormalParamUsage)
        case mc: MethodCall[V]   => mc.params.exists(hasExprFormalParamUsage)
        case be: BinaryExpr[V]   => hasExprFormalParamUsage(be.left) || hasExprFormalParamUsage(be.right)
        case _                   => false
    }

    protected def hasFormalParamUsageAlongPath(path: Path)(implicit tac: TAC): Boolean = {
        implicit val pcToIndex: Array[Int] = tac.pcToIndex
        path.elements.exists {
            case FlatPathElement(index) => tac.stmts(index) match {
                    case Assignment(_, _, expr) => hasExprFormalParamUsage(expr)
                    case ExprStmt(_, expr)      => hasExprFormalParamUsage(expr)
                    case _                      => false
                }
            case NestedPathElement(subPath, _) => hasFormalParamUsageAlongPath(Path(subPath.toList))
            case _                             => false
        }
    }

    /**
     * Finds [[PUVar]]s the string constancy information computation for the given [[Path]] depends on. Enables passing
     * an entity to ignore (usually the entity for which the path was created so it does not depend on itself).
     *
     * @return A mapping from dependent [[PUVar]]s to the [[FlatPathElement]] indices they occur in.
     */
    protected def findDependentVars(path: Path, ignore: SEntity)( // We may need to register the old path with them
        implicit state: State): mutable.LinkedHashMap[SEntity, Int] = {
        val stmts = state.tac.stmts

        def findDependeesAcc(subpath: SubPath): ListBuffer[(SEntity, Int)] = {
            val foundDependees = ListBuffer[(SEntity, Int)]()
            subpath match {
                case fpe: FlatPathElement =>
                    // For FlatPathElements, search for DUVars on which the toString method is called
                    // and where these toString calls are the parameter of an append call
                    stmts(fpe.stmtIndex(state.tac.pcToIndex)) match {
                        case ExprStmt(_, outerExpr) =>
                            if (InterpretationHandler.isStringBuilderBufferAppendCall(outerExpr)) {
                                val param = outerExpr.asVirtualFunctionCall.params.head.asVar
                                param.definedBy.filter(_ >= 0).foreach { ds =>
                                    val expr = stmts(ds).asAssignment.expr
                                    // TODO check support for passing nested string builder directly (e.g. with a test case)
                                    if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
                                        foundDependees.append((param.toPersistentForm(stmts), fpe.pc))
                                    }
                                }
                            }
                        case _ =>
                    }
                    foundDependees
                case npe: NestedPathElement =>
                    foundDependees.appendAll(npe.element.flatMap { findDependeesAcc })
                    foundDependees
                case _ => foundDependees
            }
        }

        val dependees = mutable.LinkedHashMap[SEntity, Int]()
        path.elements.foreach { nextSubpath =>
            findDependeesAcc(nextSubpath).foreach { nextPair =>
                if (ignore != nextPair._1) {
                    dependees.put(nextPair._1, nextPair._2)
                }
            }
        }
        dependees
    }
}

object StringAnalysis {

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
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[StringAnalysis.isSupportedType(String)]].
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
     *         see [[StringAnalysis.isSupportedType(String)]].
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
