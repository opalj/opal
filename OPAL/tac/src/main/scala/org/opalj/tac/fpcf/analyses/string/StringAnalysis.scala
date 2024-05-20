/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string.StringTreeParameter
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUB
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string.flowanalysis.DataFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.flowanalysis.FlowGraph
import org.opalj.tac.fpcf.analyses.string.flowanalysis.Statement
import org.opalj.tac.fpcf.analyses.string.flowanalysis.StructuralAnalysis
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
trait StringAnalysis extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(data: SContext): ProperPropertyComputationResult = {
        val state = ComputationState(declaredMethods(data._2), data, ps(data._2, TACAI.key))

        if (state.tacDependee.isRefinable) {
            InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                state.dependees.toSet,
                continuation(state)
            )
        } else if (state.tacDependee.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(state.entity, StringConstancyProperty.lb)
        } else {
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

        state.startEnv = StringTreeEnvironment(Map.empty.withDefault { pv: PV =>
            val defPCs = pv.defPCs.toList.sorted
            if (defPCs.head >= 0) {
                StringTreeNeutralElement
            } else {
                val pc = defPCs.head
                if (pc == -1 || pc <= ImmediateVMExceptionsOriginOffset) {
                    StringTreeDynamicString
                } else {
                    StringTreeParameter.forParameterPC(pc)
                }
            }
        })

        state.flowGraph = FlowGraph(tac.cfg)
        val (_, superFlowGraph, controlTree) =
            StructuralAnalysis.analyze(state.flowGraph, FlowGraph.entryFromCFG(tac.cfg))
        state.superFlowGraph = superFlowGraph
        state.controlTree = controlTree

        state.flowGraph.nodes.toOuter.foreach {
            case Statement(pc) if pc >= 0 =>
                state.updateDependee(pc, propertyStore(MethodPC(pc, state.dm), StringFlowFunction.key))

            case _ =>
        }

        computeResults
    }

    private def continuation(state: ComputationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(_: TACAI) if eps.pk.equals(TACAI.key) =>
                state.tacDependee = eps.asInstanceOf[FinalEP[Method, TACAI]]
                determinePossibleStrings(state)

            case InterimEUB(e: MethodPC) if eps.pk.equals(StringFlowFunction.key) =>
                state.updateDependee(e.pc, eps.asInstanceOf[EOptionP[MethodPC, StringFlowFunction]])
                computeResults(state)

            case _ =>
                getInterimResult(state)
        }
    }

    private def computeResults(implicit state: ComputationState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            getInterimResult(state)
        } else {
            Result(state.entity, computeNewUpperBound(state))
        }
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
        val resultEnv = DataFlowAnalysis.compute(
            state.controlTree,
            state.superFlowGraph,
            state.getFlowFunctionsByPC
        )(state.startEnv)

        StringConstancyProperty(StringConstancyInformation(resultEnv(state.entity._1)))
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
        PropertyBounds.ub(StringFlowFunction)
    )

    override final type InitializationData = StringAnalysis

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

trait LazyStringAnalysis
    extends StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, initData.analyze)

        initData
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}

sealed trait StringFlowAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringFlowFunction)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI)

    override final type InitializationData = InterpretationHandler

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}

trait LazyStringFlowAnalysis
    extends StringFlowAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        ps.registerLazyPropertyComputation(StringFlowFunction.key, initData.analyze)

        initData
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)
}
