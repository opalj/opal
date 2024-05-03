/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

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
    private def determinePossibleStrings(implicit state: ComputationState): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac

        val uVar = state.entity._1.toValueOriginForm(tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val ep = ps(
                InterpretationHandler.getEntityForDefSite(defSites.head, state.dm, tac, state.entity._1),
                StringConstancyProperty.key
            )
            if (ep.isRefinable) {
                state.dependees = ep :: state.dependees
                return InterimResult.forUB(
                    state.entity,
                    StringConstancyProperty.ub,
                    state.dependees.toSet,
                    continuation(state)
                )
            } else {
                return Result(state.entity, ep.asFinal.p)
            }
        }

        if (SimplePathFinder.containsComplexControlFlow(tac)) {
            return Result(state.entity, StringConstancyProperty.lb)
        }

        if (state.computedLeanPaths == null) {
            state.computedLeanPaths = computeLeanPaths(uVar)
        }

        state.computedLeanPaths.flatMap(getPCsInPath).distinct.foreach { pc =>
            propertyStore(
                InterpretationHandler.getEntityForPC(pc, state.dm, tac, state.entity._1),
                StringConstancyProperty.key
            ) match {
                case FinalEP(e, _) =>
                    state.dependees = state.dependees.filter(_.e != e)
                case ep =>
                    state.dependees = ep :: state.dependees
            }
        }

        if (state.dependees.isEmpty) {
            computeFinalResult(state)
        } else {
            getInterimResult(state)
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
    protected[this] def continuation(state: ComputationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(tac: TACAI) if
                    eps.pk.equals(TACAI.key) &&
                        state.tacDependee.isDefined &&
                        state.tacDependee.get == eps =>
                state.tac = tac.tac.get
                state.tacDependee = Some(eps.asInstanceOf[FinalEP[Method, TACAI]])
                determinePossibleStrings(state)

            case FinalEP(e: DUSiteEntity, _) if eps.pk.equals(StringConstancyProperty.key) =>
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
    private def computeFinalResult(state: ComputationState): Result = {
        Result(
            state.entity,
            StringConstancyProperty(StringConstancyInformation(
                tree = PathTransformer.pathsToStringTree(state.computedLeanPaths)(state, ps).simplify
            ))
        )
    }

    private def getInterimResult(state: ComputationState): InterimResult[StringConstancyProperty] = {
        InterimResult(
            state.entity,
            StringConstancyProperty.lb,
            computeNewUpperBound(state),
            state.dependees.toSet,
            continuation(state)
        )
    }

    private def computeNewUpperBound(state: ComputationState): StringConstancyProperty = {
        if (state.computedLeanPaths != null) {
            StringConstancyProperty(StringConstancyInformation(
                tree = PathTransformer.pathsToStringTree(state.computedLeanPaths)(state, ps).simplify
            ))
        } else {
            StringConstancyProperty.lb
        }
    }

    private def computeLeanPaths(value: V)(implicit tac: TAC): Seq[Path] = {
        val defSites = value.definedBy.toArray.sorted

        val call = tac.stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            computeLeanPathsForStringBuilder(value)
        } else {
            computeLeanPathsForStringConst(value)(tac.stmts)
        }
    }

    private def computeLeanPathsForStringConst(value: V)(implicit stmts: Array[Stmt[V]]): Seq[Path] =
        value.definedBy.toList.sorted.map(ds => Path(List(FlatPathElement(ds))))

    private def computeLeanPathsForStringBuilder(value: V)(implicit tac: TAC): Seq[Path] = {
        val initDefSites = InterpretationHandler.findDefSiteOfInit(value, tac.stmts)
        if (initDefSites.isEmpty) {
            Seq.empty
        } else {
            Seq(SimplePathFinder.findPath(tac).makeLeanPath(value))
        }
    }

    private def getPCsInPath(path: Path): Iterable[Int] = {
        def getDefSitesOfPathAcc(subpath: SubPath): Iterable[Int] = {
            subpath match {
                case fpe: FlatPathElement => Seq(fpe.pc)
                case _                    => Seq.empty
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
                    case entity: DUSiteEntity => initData._2.analyze(entity)
                    case _                     => throw new IllegalArgumentException(s"Unexpected entity passed for string analysis: $e")
                }
            }
        )
        initData._1
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}
