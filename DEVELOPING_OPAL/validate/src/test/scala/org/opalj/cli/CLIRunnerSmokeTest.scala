/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.scalatest.concurrent.ThreadSignaler
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import org.opalj.ai.CipherGetInstanceStrings
import org.opalj.ai.ConstantIfs
import org.opalj.ai.ExceptionUsage
import org.opalj.ai.InfiniteRecursions
import org.opalj.ai.MethodCallInformation
import org.opalj.ai.MethodReceivers
import org.opalj.ai.MethodsThatAlwaysReturnAPassedParameter
import org.opalj.ai.UselessComputations
import org.opalj.ai.domain.l0.ParameterUsageAnalysis
import org.opalj.ai.domain.l1.IfNullParameterAnalysis
import org.opalj.ai.domain.l1.MethodReturnValuesAnalysis
import org.opalj.ai.domain.l1.OwnershipAnalysis
import org.opalj.ai.domain.l1.UnusedValuesAnalysis
import org.opalj.ai.tutorial.base.IdentifyResourcesAnalysis
import org.opalj.av.viz.InstructionStatistics
import org.opalj.bi.TestResources.allManagedBITestJARs
import org.opalj.br.ClassesWithoutConcreteSubclasses
import org.opalj.br.CountClassForNameCalls
import org.opalj.br.FieldAccessInformationAnalysis
import org.opalj.br.Invokedynamics
import org.opalj.br.LoadMethodHandleOrMethodType
import org.opalj.br.LocalVariableTypeTables
import org.opalj.br.MethodAnnotationsPrinter
import org.opalj.br.NativeMethodsCounter
import org.opalj.br.OverridingMethodsCount
import org.opalj.br.PrivateMethodsWithClassTypeParameterCounter
import org.opalj.br.PublicMethodsInNonRestrictedPackagesCounter
import org.opalj.br.ShowInnerClassesInformation
import org.opalj.br.VirtualAndStaticMethodCalls
import org.opalj.br.analyses.CovariantEqualsMethodDefined
import org.opalj.br.analyses.ProjectIndexStatistics
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SimpleProjectStatistics
import org.opalj.br.analyses.observers.ObserverPatternUsage
import org.opalj.br.dependency.DependencyCount
import org.opalj.de.DependencyMatrix
import org.opalj.fpcf.analyses.FieldAccessInformationAnalysisDemo
import org.opalj.fpcf.analyses.SelfReferenceLeakageAnalysis
import org.opalj.fpcf.analyses.UnnecessarySynchronizationAnalysis
import org.opalj.fpcf.ifds.IFDSBasedVariableTypeAnalysisRunner
import org.opalj.support.debug.InterpretAllMethods
import org.opalj.support.info.CallGraph
import org.opalj.support.info.EscapeAnalysis
import org.opalj.support.info.FieldLocality
import org.opalj.support.info.Immutability
import org.opalj.support.info.MaxLocalsEvaluation
import org.opalj.support.info.PureVoidMethods
import org.opalj.support.info.Purity
import org.opalj.support.info.ReturnValueFreshness
import org.opalj.support.info.StringConstants
import org.opalj.support.info.ThrownExceptions
import org.opalj.support.info.UnusedResults
import org.opalj.support.info.Values
import org.opalj.tac.ComputeTAC
import org.opalj.tac.FieldAndArrayUsageAnalysis
import org.opalj.tac.PrintTAC
import org.opalj.tac.TAC
import org.opalj.tac.TACTemplate
import org.opalj.tac.fpcf.analyses.taint.BackwardClassForNameTaintAnalysisRunner
import org.opalj.tac.fpcf.analyses.taint.ForwardClassForNameTaintAnalysisRunner

class CLIRunnerSmokeTest extends AnyFunSpec {

    val cliRunners: Seq[ProjectsAnalysisApplication] = Seq(
        SimpleProjectStatistics,
        PublicMethodsInNonRestrictedPackagesCounter,
        MethodReceivers,
        Immutability,
        MaxLocalsEvaluation,
        EscapeAnalysis,
        Values,
        LoadMethodHandleOrMethodType,
        ParameterUsageAnalysis,
        Purity,
        Invokedynamics,
        ShowInnerClassesInformation,
        // AvailableExpressions, // Requires a method to be specified
        // VeryBusyExpressions, // Requires a method to be specified
        OverridingMethodsCount,
        UselessComputations,
        SelfReferenceLeakageAnalysis,
        ObserverPatternUsage,
        ProjectIndexStatistics,
        InstructionStatistics,
        CipherGetInstanceStrings,
        LocalVariableTypeTables,
        UnusedValuesAnalysis,
        FieldAccessInformationAnalysisDemo,
        UnnecessarySynchronizationAnalysis,
        IdentifyResourcesAnalysis,
        ConstantIfs,
        IfNullParameterAnalysis,
        StringConstants,
        FieldAndArrayUsageAnalysis,
        InterpretAllMethods,
        // TACAItoGraphs, // Requires output dir for graphs
        // TransitiveUsage, // Requires a class to be specified
        MethodReturnValuesAnalysis,
        UnusedResults,
        VirtualAndStaticMethodCalls,
        MethodsThatAlwaysReturnAPassedParameter,
        PureVoidMethods,
        FieldAccessInformationAnalysis,
        // DependencyAnalysis, // Opens a file on execution
        // ClassUsageAnalysis, // Requires a class to be specified
        InfiniteRecursions,
        CovariantEqualsMethodDefined,
        ThrownExceptions,
        ReturnValueFreshness,
        ClassesWithoutConcreteSubclasses,
        NativeMethodsCounter,
        PrivateMethodsWithClassTypeParameterCounter,
        CallGraph,
        CountClassForNameCalls,
        ExceptionUsage,
        OwnershipAnalysis,
        MethodAnnotationsPrinter,
        DependencyCount,
        FieldLocality,
        MethodCallInformation,
        BackwardClassForNameTaintAnalysisRunner,
        ForwardClassForNameTaintAnalysisRunner,
        // ClassFileInformation, // Requires a class to be specified
        // ClassHierarchyVisualizer, // Opens a file on execution
        ComputeTAC,
        DependencyMatrix,
        IFDSBasedVariableTypeAnalysisRunner,
        // InterpretMethods,
        // LocalPointsTo, // Requires a class and signature to be specified
        // PrintBaseCFG, // Requires a method to be specified
        PrintTAC,
        // ProjectSerializer, // Requires an output dir to be specified
        TAC,
        // TACAItoGraphs, // Requires an output dir to be specified
        TACTemplate
    )

    describe("executing a command line runner should not fail") {
        allManagedBITestJARs() foreach { biProject =>
            cliRunners foreach { runner =>
                it(s"$runner for $biProject") {
                    failAfter(10.seconds) {
                        runner.main(Array("--cp", biProject.getAbsolutePath, "--noJDK"))
                    }(ThreadSignaler)
                }
            }
        }
    }
}
