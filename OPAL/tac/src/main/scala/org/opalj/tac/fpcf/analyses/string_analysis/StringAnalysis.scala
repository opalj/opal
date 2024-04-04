/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathType
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SimplePathFinder
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath
import org.opalj.tac.fpcf.properties.TACAI

/**
 * String Analysis trait defining some basic dependency handling.
 *
 * @author Maximilian Rüsch
 */
trait StringAnalysis extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(data: SContext): ProperPropertyComputationResult = {
        val state = ComputationState(declaredMethods(data._2), data)

        val tacaiEOptP = ps(data._2, TACAI.key)
        if (tacaiEOptP.isRefinable) {
            state.tacDependee = Some(tacaiEOptP)
            getInterimResult(state)
        } else if (tacaiEOptP.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(state.entity, StringConstancyProperty.lb)
        } else {
            state.tac = tacaiEOptP.ub.tac.get
            determinePossibleStrings(state)
        }
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    protected[string_analysis] def determinePossibleStrings(implicit
        state: ComputationState
    ): ProperPropertyComputationResult

    /**
     * Continuation function for this analysis.
     *
     * @param state The current computation state. Within this continuation, dependees of the state
     *              might be updated. Furthermore, methods processing this continuation might alter
     *              the state.
     * @return Returns a final result if (already) available. Otherwise, an intermediate result will
     *         be returned.
     */
    protected[this] def continuation(state: ComputationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(tac: TACAI) if
                    eps.pk.equals(TACAI.key) &&
                        state.tacDependee.isDefined &&
                        state.tacDependee.get == eps =>
                state.tac = tac.tac.get
                state.tacDependee = Some(eps.asInstanceOf[FinalEP[Method, TACAI]])
                determinePossibleStrings(state)

            case FinalEP(e, _) if eps.pk.equals(StringConstancyProperty.key) =>
                state.dependees = state.dependees.filter(_.e != e)

                // No more dependees => Return the result for this analysis run
                if (state.dependees.isEmpty) {
                    computeFinalResult(state)
                } else {
                    getInterimResult(state)
                }
            case _ =>
                getInterimResult(state)
        }
    }

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
    protected def computeFinalResult(state: ComputationState): Result = {
        Result(
            state.entity,
            StringConstancyProperty(StringConstancyInformation(
                tree = PathTransformer.pathToStringTree(state.computedLeanPath)(state, ps).simplify
            ))
        )
    }

    protected def getInterimResult(state: ComputationState): InterimResult[StringConstancyProperty] = {
        InterimResult(
            state.entity,
            StringConstancyProperty.lb,
            computeNewUpperBound(state),
            state.dependees.toSet,
            continuation(state)
        )
    }

    private def computeNewUpperBound(state: ComputationState): StringConstancyProperty = {
        if (state.computedLeanPath != null) {
            StringConstancyProperty(StringConstancyInformation(
                tree = PathTransformer.pathToStringTree(state.computedLeanPath)(state, ps).simplify
            ))
        } else {
            StringConstancyProperty.lb
        }
    }

    /**
     * This function traverses the given path, computes all string values along the path and stores
     * these information in the given state.
     *
     * @param p     The path to traverse.
     * @param state The current state of the computation.
     * @return Returns `true` if all values computed for the path are final results.
     */
    protected def computeResultsForPath(p: Path)(implicit state: ComputationState): Boolean = {
        var hasFinalResult = true
        p.elements.foreach {
            case fpe: FlatPathElement =>
                val eOptP =
                    ps(InterpretationHandler.getEntityForPC(fpe.pc, state.dm, state.tac), StringConstancyProperty.key)
                if (eOptP.isRefinable) {
                    hasFinalResult = false
                }
            case npe: NestedPathElement =>
                hasFinalResult = hasFinalResult && computeResultsForPath(Path(npe.element.toList))(state)
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
            NestedPathElement(
                defSites.toIndexedSeq.map { ds => NestedPathElement(Seq(FlatPathElement(ds)), None) },
                Some(NestedPathType.CondWithAlternative)
            )
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
            Some(SimplePathFinder.findPath(tac).makeLeanPath(value))
        }
    }

    /**
     * Finds [[PUVar]]s the string constancy information computation for the given [[Path]] depends on. Enables passing
     * an entity to ignore (usually the entity for which the path was created so it does not depend on itself).
     *
     * @return A mapping from dependent [[PUVar]]s to the [[FlatPathElement]] indices they occur in.
     */
    protected def findDependentVars(path: Path, ignore: SEntity)( // We may need to register the old path with them
        implicit state: ComputationState): mutable.LinkedHashMap[SEntity, Int] = {
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

    protected def getPCsInPath(path: Path): Iterable[Int] = {
        def getDefSitesOfPathAcc(subpath: SubPath): Iterable[Int] = {
            subpath match {
                case fpe: FlatPathElement   => Seq(fpe.pc)
                case npe: NestedPathElement => npe.element.flatMap(getDefSitesOfPathAcc)
                case _                      => Seq.empty
            }
        }

        path.elements.flatMap(getDefSitesOfPathAcc)
    }
}

object StringAnalysis {

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

    def isSupportedType(fieldType: FieldType): Boolean = isSupportedType(fieldType.toJava)

    def getDynamicStringInformationForNumberType(numberType: String): StringConstancyInformation = {
        numberType match {
            case "short" | "int"    => StringConstancyInformation.dynamicInt
            case "float" | "double" => StringConstancyInformation.dynamicFloat
            case _                  => StringConstancyInformation.lb
        }
    }
}

sealed trait StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.lub(StringConstancyProperty)
    )

    override final type InitializationData = (StringAnalysis, InterpretationHandler)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

trait LazyStringAnalysis
    extends StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(
            StringConstancyProperty.key,
            (e: Entity) => {
                e match {
                    case _: (_, _)             => initData._1.analyze(e.asInstanceOf[SContext])
                    case entity: DefSiteEntity => initData._2.analyze(entity)
                    case _                     => throw new IllegalArgumentException(s"Unexpected entity passed for string analysis: $e")
                }
            }
        )
        initData._1
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}
