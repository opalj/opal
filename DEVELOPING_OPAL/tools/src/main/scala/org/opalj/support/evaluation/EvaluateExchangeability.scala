/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package evaluation
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.properties.ContextuallyPure
import org.opalj.br.fpcf.properties.ContextuallySideEffectFree
import org.opalj.br.fpcf.properties.DContextuallyPure
import org.opalj.br.fpcf.properties.DContextuallySideEffectFree
import org.opalj.br.fpcf.properties.DPure
import org.opalj.br.fpcf.properties.DSideEffectFree
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.properties.ImpureByLackOfInformation
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Usage:
 *   "-cp=x", where x is the path to the target
 *   "-libCP=x", where x is the path to the library
 *   "-includeJDK"
 *   "-closedWorld"
 *   "-mode=x", where x is either library or application
 *   "-Px", where x is in {0, 1, 2} to enable purity analysis
 *   "-Ex", where x is in {0, 1} to enable escape analysis
 *   "-FMx", where x is in {0, 1, 2} to enable field mutability analysis
 *   "-RVF", to enable return value freshness analysis
 *   "-FL", to enable field locality analysis
 *   "-CI", to enable class immutability analysis
 *   "-TI", to enable type immutability analysis
 *
 * @author Florian Kuebler
 */
object EvaluateExchangeability {
    def main(args: Array[String]): Unit = {

        val cpReg = "-cp=(.*)".r
        val libReg = "-libCP=(.*)".r
        val evalDirReg = "-eval=(.*)".r
        val purityReg = "-P([012])".r
        val escapeReg = "-E([012])".r
        val fieldMutabilityReg = "-FM([012])".r

        var cp: File = null
        var libPath: Option[File] = None
        var includeJDK: Boolean = false
        var isLibrary: Boolean = false
        var closedWorldAssumption: Boolean = false

        var evaluationDir: Option[File] = None

        var purityAnalysis: Option[Byte] = None
        var escapeAnalysis: Option[Byte] = None
        var fieldMutabilityAnalysis: Option[Byte] = None
        var returnValueFreshnessAnalysis: Boolean = false
        var fieldLocalityAnalysis: Boolean = false
        var classImmutabilityAnalysis: Boolean = false
        var typeImmutabilityAnalysis: Boolean = false

        var projectTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        // todo ensure that no value is set twice
        args.foreach {
            case cpReg(path) ⇒
                if (cp ne null)
                    throw new IllegalArgumentException()
                cp = new File(path)

            case libReg(path) ⇒
                if (libPath.isDefined)
                    throw new IllegalArgumentException()
                libPath = Some(new File(path))
            case evalDirReg(path) ⇒
                if (evaluationDir.isDefined)
                    throw new IllegalArgumentException()
                else evaluationDir = Some(new File(path))
            case "-includeJDK"             ⇒ includeJDK = true
            case "-closedWorld"            ⇒ closedWorldAssumption = true
            case "-mode=application"       ⇒ isLibrary = false
            case "-mode=library"           ⇒ isLibrary = true
            case purityReg(level)          ⇒ purityAnalysis = Some(level.toByte)
            case escapeReg(level)          ⇒ escapeAnalysis = Some(level.toByte)
            case fieldMutabilityReg(level) ⇒ fieldMutabilityAnalysis = Some(level.toByte)
            case "-RVF"                    ⇒ returnValueFreshnessAnalysis = true
            case "-FL"                     ⇒ fieldLocalityAnalysis = true
            case "-CI"                     ⇒ classImmutabilityAnalysis = true
            case "-TI"                     ⇒ typeImmutabilityAnalysis = true
        }

        val dirName = if (cp.getAbsolutePath == org.opalj.bytecode.JRELibraryFolder.getAbsolutePath)
            "JDK" else cp.getName
        val projectEvalDir = evaluationDir.map(new File(_, dirName))
        if (projectEvalDir.isDefined && !projectEvalDir.get.exists()) projectEvalDir.get.mkdir()

        val baseConfig = if (closedWorldAssumption) BaseConfig.withValue(
            "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
        )
        else BaseConfig

        implicit val config: Config = if (isLibrary) {
            baseConfig.withValue(
                "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryEntryPointsFinder")
            ).withValue(
                    "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey.analysis",
                    ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryInstantiatedTypesFinder")
                )
        } else {
            baseConfig.withValue(
                "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
            ).withValue(
                    "org.opalj.br.analyses.cg.InitialInstantiatedTypesKey.analysis",
                    ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder")
                )
        }

        val classFiles = JavaClassFileReader().ClassFiles(cp)

        val libFiles = libPath match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(dir)
            case None      ⇒ Traversable.empty
        }

        val JDKFiles = if (!includeJDK) Traversable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        val project = time {
            Project(
                classFiles,
                libFiles ++ JDKFiles,
                libraryClassFilesAreInterfacesOnly = false,
                Traversable.empty
            )
        } { t ⇒ projectTime = t.toSeconds }

        project.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(classOf[DefaultDomainWithCFGAndDefUse[_]])
                case Some(requirements) ⇒ requirements + classOf[DefaultDomainWithCFGAndDefUse[_]]
            }): Set[Class[_ <: AnyRef]]
        )

        val ps = time { project.get(PropertyStoreKey) } { t ⇒ propertyStoreTime = t.toSeconds }

        val manager = project.get(FPCFAnalysesManagerKey)

        val cgAnalyses = Set[ComputationSpecification[FPCFAnalysis]](
            RTACallGraphAnalysisScheduler,
            TriggeredStaticInitializerAnalysis,
            TriggeredLoadedClassesAnalysis,
            TriggeredFinalizerAnalysisScheduler,
            TriggeredThreadRelatedCallsAnalysis,
            TriggeredSerializationRelatedCallsAnalysis,
            TriggeredReflectionRelatedCallsAnalysis,
            TriggeredInstantiatedTypesAnalysis,
            TriggeredConfiguredNativeMethodsAnalysis,
            TriggeredSystemPropertiesAnalysis,
            LazyL0BaseAIAnalysis,
            TACAITransformer,
            LazyCalleesAnalysis(
                Set(
                    StandardInvokeCallees,
                    SerializationRelatedCallees,
                    ReflectionRelatedCallees,
                    ThreadRelatedIncompleteCallSites
                )
            )
        )

        time {
            if (isLibrary) {
                manager.runAll(cgAnalyses + EagerLibraryEntryPointsAnalysis)
            } else {
                manager.runAll(cgAnalyses)
            }
        } { t ⇒ callGraphTime = t.toSeconds }

        var analyses: List[FPCFAnalysisScheduler] = Nil

        if (purityAnalysis.isDefined) {
            purityAnalysis.get match {
                case 0 ⇒ analyses ::= EagerL0PurityAnalysis

                case 1 ⇒
                    L1PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))
                    analyses ::= EagerL1PurityAnalysis
                case 2 ⇒
                    L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))
                    analyses ::= EagerL2PurityAnalysis
            }
        }

        if (escapeAnalysis.isDefined) {
            escapeAnalysis.get match {
                case 0 ⇒ analyses ::= LazySimpleEscapeAnalysis
                case 1 ⇒ analyses ::= LazyInterProceduralEscapeAnalysis
            }
        }

        if (fieldMutabilityAnalysis.isDefined) {
            fieldMutabilityAnalysis.get match {
                case 0 ⇒ analyses ::= LazyL0FieldMutabilityAnalysis
                case 1 ⇒ analyses ::= LazyL1FieldMutabilityAnalysis
                case 2 ⇒ analyses ::= LazyL2FieldMutabilityAnalysis
            }
        }

        if (returnValueFreshnessAnalysis)
            analyses ::= LazyReturnValueFreshnessAnalysis

        if (fieldLocalityAnalysis)
            analyses ::= LazyFieldLocalityAnalysis

        if (classImmutabilityAnalysis)
            analyses ::= LazyClassImmutabilityAnalysis

        if (typeImmutabilityAnalysis)
            analyses ::= LazyTypeImmutabilityAnalysis

        time {
            manager.runAll(analyses)

        } { t ⇒ analysisTime = t.toSeconds }

        ps.shutdown()

        val declaredMethods = project.get(DeclaredMethodsKey)

        val projMethods: Seq[DefinedMethod] =
            for (cf ← project.allProjectClassFiles; m ← cf.methodsWithBody)
                yield declaredMethods(m)

        val reachableMethods =
            ps.entities(CallersProperty.key).collect {
                case FinalEP(e: DeclaredMethod, c: CallersProperty) if c ne NoCallers ⇒ e
            }.toSet

        val analyzedMethods = projMethods.filter(reachableMethods.contains)

        if (projectEvalDir.isDefined) {
            val runtime = new File(projectEvalDir.get, "runtime.csv")
            val runtimeNew = !runtime.exists()
            val runtimeWriter = new PrintWriter(new FileOutputStream(runtime, true))
            try {
                if (runtimeNew) {
                    runtime.createNewFile()
                    runtimeWriter.println("project;tac;propertyStore;callGraph;analysis")
                }
                runtimeWriter.println(s"$projectTime;$propertyStoreTime;$callGraphTime;$analysisTime")
            } finally {
                if (runtimeWriter != null) runtimeWriter.close()
            }
        }

        val purityEs = ps(analyzedMethods, br.fpcf.properties.Purity.key).filter {
            case FinalP(p) ⇒ p ne ImpureByLackOfInformation
            case ep        ⇒ throw new RuntimeException(s"non final purity result $ep")
        }

        def isExternal(dm: DefinedMethod, p: IntTrieSet): Boolean = {
            !dm.definedMethod.isStatic && p.size == 1 && p.head == 0
        }

        val compileTimePure = purityEs.collect { case FinalEP(m: DefinedMethod, CompileTimePure) ⇒ m }
        val pure = purityEs.collect { case FinalEP(m: DefinedMethod, Pure) ⇒ m }
        val sideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, SideEffectFree) ⇒ m }
        val externallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallyPure(p)) if isExternal(m, p) ⇒ m }
        val externallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallySideEffectFree(p)) if isExternal(m, p) ⇒ m }
        val contextuallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallyPure(p)) if !isExternal(m, p) ⇒ (m, p) }
        val contextuallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallySideEffectFree(p)) if !isExternal(m, p) ⇒ (m, p) }
        val dPure = purityEs.collect { case FinalEP(m: DefinedMethod, DPure) ⇒ m }
        val dSideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DSideEffectFree) ⇒ m }
        val dExternallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallyPure(p)) if isExternal(m, p) ⇒ m }
        val dExternallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallySideEffectFree(p)) if isExternal(m, p) ⇒ m }
        val dContextuallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallyPure(p)) if !isExternal(m, p) ⇒ (m, p) }
        val dContextuallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallySideEffectFree(p)) if !isExternal(m, p) ⇒ (m, p) }
        val lbImpure = purityEs.collect { case FinalEP(m: DefinedMethod, ImpureByAnalysis) ⇒ m }

        if (projectEvalDir.isDefined) {
            val results = new File(projectEvalDir.get, "method-results.csv")
            val resultsNew = !results.exists()
            val resultsWriter = new PrintWriter(new FileOutputStream(results, true))
            try {
                if (resultsNew) {
                    results.createNewFile()
                    resultsWriter.println("compile time pure;pure;domain-specific pure;"+
                        "side-effect free;domain-specific side-effect free;"+
                        "externally pure;domain-specific externally pure;"+
                        "externally side-effect free; domain-specific externally side-effect "+
                        "free;contextually pure;domain-specific contextually pure;"+
                        "contextually side-effect free;domain-specific contextually "+
                        "side-effect free;impure;count")

                    resultsWriter.println(
                        s"${compileTimePure.size};${pure.size};${dPure.size};"+
                            s"${sideEffectFree.size};${dSideEffectFree.size};"+
                            s"${externallyPure.size};${dExternallyPure.size};"+
                            s"${contextuallyPure.size};${dContextuallyPure.size};"+
                            s"${externallySideEffectFree.size};"+
                            s"${dExternallySideEffectFree.size};"+
                            s"${contextuallySideEffectFree.size};"+
                            s"${dContextuallySideEffectFree.size};"+
                            s"${lbImpure.size};${purityEs.size}"
                    )
                }
            } finally {
                if (resultsWriter != null) resultsWriter.close()
            }
        } else {
            val result =
                ps.toString(false)+
                    "\ncompile-time pure:                     "+compileTimePure.size+
                    "\nAt least pure:                         "+pure.size+
                    "\nAt least domain-specficic pure:        "+dPure.size+
                    "\nAt least side-effect free:             "+sideEffectFree.size+
                    "\nAt least d-s side effect free:         "+dSideEffectFree.size+
                    "\nAt least externally pure:              "+externallyPure.size+
                    "\nAt least d-s externally pure:          "+dExternallyPure.size+
                    "\nAt least externally side-effect free:  "+externallySideEffectFree.size+
                    "\nAt least d-s ext. side-effect free:    "+dExternallySideEffectFree.size+
                    "\nAt least contextually pure:            "+contextuallyPure.size+
                    "\nAt least d-s contextually pure:        "+dContextuallyPure.size+
                    "\nAt least contextually side-effect free:"+contextuallySideEffectFree.size+
                    "\nAt least d-s cont. side-effect free:   "+dContextuallySideEffectFree.size+
                    "\nImpure:                                "+lbImpure.size+
                    "\nTotal:                                 "+purityEs.size
            Console.println(result)
            Console.println(s"Call-graph time: $callGraphTime")
            Console.println(s"Analysis time: $analysisTime")
        }
    }
}
