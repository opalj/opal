/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.ai.FormalParametersOriginOffset
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.string.StringConstancyInformationConst
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.br.fpcf.properties.string.StringTreeParameter
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string.preprocessing.PathElement
import org.opalj.tac.fpcf.analyses.string.preprocessing.PathFinder
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.ConstantResultFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunction

/**
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

        // TODO put a function parameter with their parameter string tree into the flow analysis
        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            // TODO what do we do with string builder parameters?
            if (pc <= FormalParametersOriginOffset) {
                if (pc == -1 || pc <= ImmediateVMExceptionsOriginOffset) {
                    return Result(FinalEP(InterpretationHandler.getEntity(state), sff))

                    return StringInterpreter.computeFinalLBFor(state.entity._1)
                } else {
                    return StringInterpreter.computeFinalResult(ConstantResultFlow.forVariable(
                        state.entity._1,
                        StringTreeParameter.forParameterPC(pc)
                    ))
                }
            }

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

        if (state.computedLeanPaths == null) {
            state.computedLeanPaths = computeLeanPaths(uVar)
        }

        if (state.computedLeanPaths.isEmpty) {
            return Result(state.entity, StringConstancyProperty.lb)
        }

        state.computedLeanPaths.flatMap(_.elements.map(_.pc)).distinct.foreach { pc =>
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
     *              For each [[PathElement]], there must be a corresponding entry in
     *              `state.fpe2sci`. If this criteria is not met, a [[NullPointerException]] will
     *              be thrown (in this case there was some work to do left and this method should
     *              not have been called)!
     * @return Returns the final result.
     */
    private def computeFinalResult(state: ComputationState): Result = Result(state.entity, computeNewUpperBound(state))

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
            val reducedStringTree = if (state.computedLeanPaths.isEmpty) {
                StringTreeNeutralElement
            } else {
                StringTreeOr(state.computedLeanPaths.map { PathFinder.transformPath(_, state.tac)(state, ps) })
            }

            StringConstancyProperty(StringConstancyInformationConst(reducedStringTree.simplify))
        } else {
            StringConstancyProperty.lb
        }
    }

    private def computeLeanPaths(value: V)(implicit tac: TAC): Seq[Path] = {
        if (value.value.isReferenceValue && (
                value.value.asReferenceValue.asReferenceType.mostPreciseObjectType == ObjectType.StringBuilder
                || value.value.asReferenceValue.asReferenceType.mostPreciseObjectType == ObjectType.StringBuffer
            )
        ) {
            PathFinder.findPath(value, tac).map(Seq(_)).getOrElse(Seq.empty)
        } else {
            value.definedBy.toList.sorted.map(ds => Path(List(PathElement(ds)(tac.stmts))))
        }
    }
}

object StringAnalysis {

    /**
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    private def isSupportedPrimitiveNumberType(typeName: String): Boolean =
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
        PropertyBounds.ub(StringFlowFunction),
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
        // TODO double register lazy computation for pc scoped entities as well
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, initData._1.analyze)
        ps.registerLazyPropertyComputation(StringFlowFunction.key, initData._2.analyze)

        initData._1
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}
