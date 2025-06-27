/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.language.postfixOps

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Calendar

import org.opalj.ai.domain.DomainCommand
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectBasedCommandLineConfig
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.ContextuallyPure
import org.opalj.br.fpcf.properties.ContextuallySideEffectFree
import org.opalj.br.fpcf.properties.DContextuallyPure
import org.opalj.br.fpcf.properties.DContextuallySideEffectFree
import org.opalj.br.fpcf.properties.DPure
import org.opalj.br.fpcf.properties.DSideEffectFree
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.ImpureByLackOfInformation
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.bytecode.JDKCommand
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.cli.AnalysisLevelCommand
import org.opalj.cli.ClassPathCommand
import org.opalj.cli.ClosedWorldCommand
import org.opalj.cli.ConfigurationNameCommand
import org.opalj.cli.EagerCommand
import org.opalj.cli.EvalDirCommand
import org.opalj.cli.IndividualCommand
import org.opalj.cli.LibraryCommand
import org.opalj.cli.MultiProjectsCommand
import org.opalj.cli.PackagesCommand
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.par.SchedulingStrategyCommand
import org.opalj.tac.cg.CallGraphCommand
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.EscapeCommand
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.FieldAssignabilityCommand
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.PurityCommand
import org.opalj.tac.fpcf.analyses.purity.RaterCommand
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import org.rogach.scallop.ScallopConf

class PurityConfig(args: Array[String]) extends ScallopConf(args) with ProjectBasedCommandLineConfig
    with PropertyStoreBasedCommandLineConfig {

    private val analysisLevelCommand = new AnalysisLevelCommand(PurityCommand.description, PurityCommand.levels *) {
        override def defaultValue: Option[String] = Some("L2")
        override val withNone = false
    }

    commands(
        analysisLevelCommand !,
        FieldAssignabilityCommand,
        EscapeCommand,
        EagerCommand !,
        DomainCommand,
        ConfigurationNameCommand !,
        RaterCommand !,
        CallGraphCommand !,
        IndividualCommand !,
        ClosedWorldCommand,
        MultiProjectsCommand !,
        EvalDirCommand,
        PackagesCommand,
        SchedulingStrategyCommand
    )
    init()

    val analysis = getScheduler(apply(analysisLevelCommand), false).asInstanceOf[FPCFAnalysisScheduler]

    val supportingAnalyses: List[FPCFAnalysisScheduler] = parseArgumentsForSupport(
        get(FieldAssignabilityCommand),
        get(EscapeCommand),
        apply(EagerCommand)
    )

    private def parseArgumentsForSupport(
        fieldAssignability: Option[String],
        escape:             Option[String],
        eager:              Boolean
    ) = {
        var support: List[org.opalj.fpcf.FPCFAnalysisScheduler[_]] = Nil

        if (analysis eq LazyL2PurityAnalysis) support = List(
            LazyFieldImmutabilityAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )

        support ::= EagerFieldAccessInformationAnalysis

        if (eager) {
            support ::= EagerClassImmutabilityAnalysis
            support ::= EagerTypeImmutabilityAnalysis
        } else {
            support ::= LazyClassImmutabilityAnalysis
            support ::= LazyTypeImmutabilityAnalysis
        }

        escape match {
            case Some("") =>
            case _ =>
                escape.orElse(Some(EscapeCommand.parse("L1"))).foreach { e => support ::= getScheduler(e, eager) }
        }

        fieldAssignability match {
            case Some("") =>
            case Some(fA) => support ::= getScheduler(fA, eager)
            case None => analysis match {
                    case LazyL0PurityAnalysis => support ::= LazyL0FieldAssignabilityAnalysis
                    case LazyL1PurityAnalysis => support ::= LazyL1FieldAssignabilityAnalysis
                    case LazyL2PurityAnalysis => support ::= LazyL1FieldAssignabilityAnalysis
                }
        }

        support.asInstanceOf[List[FPCFAnalysisScheduler]]
    }
}

/**
 * Executes a purity analysis (L2 by default) along with necessary supporting analysis.
 *
 * @author Dominik Helm
 */
object Purity {

    private val JDKPackages = List(
        "java/",
        "javax",
        "javafx",
        "jdk",
        "sun",
        "oracle",
        "com/sun",
        "netscape",
        "org/ietf/jgss",
        "org/jcp/xml/dsig/internal",
        "org/omg",
        "org/w3c/dom",
        "org/xml/sax"
    )

    def evaluate(
        analysisConfig: PurityConfig,
        cp:             File
    ): Unit = {
        val isJDK: Boolean = cp eq JRELibraryFolder
        val dirName = if (isJDK) "JDK" else cp.getName
        val projectEvalDir = analysisConfig.get(EvalDirCommand).map(new File(_, dirName))
        if (projectEvalDir.isDefined && !projectEvalDir.get.exists()) projectEvalDir.get.mkdir()

        var projectTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        val isLibrary = analysisConfig(LibraryCommand) || isJDK
        val project = time {
            analysisConfig.setupProject(cp, isLibrary)
        } { t => projectTime = t.toSeconds }

        val ps = time {
            analysisConfig.setupPropertyStore()
            project.get(PropertyStoreKey)
        } { t => propertyStoreTime = t.toSeconds }

        val rater = analysisConfig(RaterCommand)
        analysisConfig.analysis match {
            case LazyL0PurityAnalysis =>
            case LazyL1PurityAnalysis => L1PurityAnalysis.setRater(Some(rater))
            case LazyL2PurityAnalysis => L2PurityAnalysis.setRater(Some(rater))
        }

        val declaredMethods = project.get(DeclaredMethodsKey)

        val allMethods: Iterable[DefinedMethod] =
            for (cf <- project.allProjectClassFiles; m <- cf.methodsWithBody)
                yield declaredMethods(m)

        val packages = analysisConfig.get(PackagesCommand)

        val projMethods = allMethods.filter { m =>
            val pn = m.definedMethod.classFile.thisType.packageName
            packages match {
                case None => isJDK || !JDKPackages.exists(pn.startsWith)
                case Some(ps) =>
                    ps.exists(pn.startsWith)
            }
        }

        val manager = project.get(FPCFAnalysesManagerKey)

        time {
            project.get(analysisConfig(CallGraphCommand))
        } { t => callGraphTime = t.toSeconds }

        val reachableMethods =
            ps.entities(Callers.key).collect {
                case FinalEP(m: DeclaredMethod, c: Callers) if c ne NoCallers => m
            }.toSet

        val contextProvider = project.get(ContextProviderKey)
        val analyzedContexts = projMethods.filter(reachableMethods.contains).map(contextProvider.newContext(_))

        time {
            val analyses = analysisConfig.analysis :: analysisConfig.supportingAnalyses

            manager.runAll(
                analyses,
                css =>
                    if (css.contains(analysisConfig.analysis)) {
                        analyzedContexts.foreach { dm => ps.force(dm, br.fpcf.properties.Purity.key) }
                    }
            )
        } { t => analysisTime = t.toSeconds }
        ps.shutdown()

        val entitiesWithPurity = ps(analyzedContexts, br.fpcf.properties.Purity.key).filter {
            case FinalP(p) => p ne ImpureByLackOfInformation
            case ep        => throw new RuntimeException(s"non final purity result $ep")
        }

        val projectEntitiesWithPurity = entitiesWithPurity.filter { ep =>
            val pn = ep.e.asInstanceOf[Context].method.declaringClassType.asClassType.packageName
            packages match {
                case None     => isJDK || !JDKPackages.exists(pn.startsWith)
                case Some(ps) => ps.exists(pn.startsWith)
            }
        }.toSeq

        def isExternal(dm: DefinedMethod, p: IntTrieSet): Boolean = {
            !dm.definedMethod.isStatic && p.size == 1 && p.head == 0
        }

        val compileTimePure =
            projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), CompileTimePure) => m }
        val pure = projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), Pure) => m }
        val sideEffectFree =
            projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), SideEffectFree) => m }
        val externallyPure = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), ContextuallyPure(p)) if isExternal(m, p) => m
        }
        val externallySideEffectFree = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), ContextuallySideEffectFree(p)) if isExternal(m, p) => m
        }
        val contextuallyPure = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), ContextuallyPure(p)) if !isExternal(m, p) => (m, p)
        }
        val contextuallySideEffectFree = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), ContextuallySideEffectFree(p)) if !isExternal(m, p) => (m, p)
        }
        val dPure = projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), DPure) => m }
        val dSideEffectFree =
            projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), DSideEffectFree) => m }
        val dExternallyPure = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), DContextuallyPure(p)) if isExternal(m, p) => m
        }
        val dExternallySideEffectFree = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), DContextuallySideEffectFree(p)) if isExternal(m, p) => m
        }
        val dContextuallyPure = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), DContextuallyPure(p)) if !isExternal(m, p) => (m, p)
        }
        val dContextuallySideEffectFree = projectEntitiesWithPurity.collect {
            case FinalEP(Context(m: DefinedMethod), DContextuallySideEffectFree(p)) if !isExternal(m, p) => (m, p)
        }
        val lbImpure = projectEntitiesWithPurity.collect { case FinalEP(Context(m: DefinedMethod), ImpureByAnalysis) =>
            m
        }

        if (projectEvalDir.isDefined) {
            val configurationName = analysisConfig(ConfigurationNameCommand)

            // WRITE ANALYSIS OUTPUT

            val output = new File(projectEvalDir.get, "purityResults.csv")
            val newFile = !output.exists()
            val outputWriter = new PrintWriter(new FileOutputStream(output, true))
            try {
                if (newFile) {
                    output.createNewFile()
                    outputWriter.println(
                        "analysisName;project time;propertyStore time;" +
                            "callGraph time;analysis time; total time;" +
                            "compile time pure;pure;domain-specific pure;" +
                            "side-effect free;domain-specific side-effect free;" +
                            "externally pure;domain-specific externally pure;" +
                            "externally side-effect free; domain-specific externally side-effect " +
                            "free;contextually pure;domain-specific contextually pure;" +
                            "contextually side-effect free;domain-specific contextually " +
                            "side-effect free;impure;count"
                    )
                }
                val totalTime = projectTime + propertyStoreTime + callGraphTime + analysisTime
                outputWriter.println(
                    s"$configurationName;${projectTime.toString(false)};" +
                        s"${propertyStoreTime.toString(false)};" +
                        s"${callGraphTime.toString(false)};" +
                        s"${analysisTime.toString(false)};" +
                        s"${totalTime.toString(false)};" +
                        s"${compileTimePure.size};${pure.size};${dPure.size};" +
                        s"${sideEffectFree.size};${dSideEffectFree.size};" +
                        s"${externallyPure.size};${dExternallyPure.size};" +
                        s"${contextuallyPure.size};${dContextuallyPure.size};" +
                        s"${externallySideEffectFree.size};" +
                        s"${dExternallySideEffectFree.size};" +
                        s"${contextuallySideEffectFree.size};" +
                        s"${dContextuallySideEffectFree.size};" +
                        s"${lbImpure.size};${projectEntitiesWithPurity.size}"
                )
            } finally {
                if (outputWriter != null) outputWriter.close()
            }

            // WRITE CONTENT INFORMATION

            val aggregated = !analysisConfig(IndividualCommand)

            val results = new File(projectEvalDir.get, "method-results.csv")
            val resultsNew = !results.exists()
            val resultsWriter = new PrintWriter(new FileOutputStream(results, aggregated))
            try {
                if (resultsNew) {
                    results.createNewFile()
                    if (aggregated)
                        resultsWriter.println("analysisName;compile time pure;pure;" +
                            "domain-specific pure;side-effect free;" +
                            "domain-specific side-effect free;externally pure;" +
                            "domain-specific externally pure;externally side-effect free;" +
                            "domain-specific externally side-effect free;" +
                            "contextually pure;domain-specific contextually pure;" +
                            "contextually side-effect free;domain-specific contextually " +
                            "side-effect free;impure;count")
                }

                if (aggregated) {
                    resultsWriter.println(
                        s"$configurationName;${compileTimePure.size};${pure.size};${dPure.size};" +
                            s"${sideEffectFree.size};${dSideEffectFree.size};" +
                            s"${externallyPure.size};${dExternallyPure.size};" +
                            s"${contextuallyPure.size};${dContextuallyPure.size};" +
                            s"${externallySideEffectFree.size};" +
                            s"${dExternallySideEffectFree.size};" +
                            s"${contextuallySideEffectFree.size};" +
                            s"${dContextuallySideEffectFree.size};" +
                            s"${lbImpure.size};${projectEntitiesWithPurity.size}"
                    )
                } else {
                    for (m <- compileTimePure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => compile time pure")
                    }
                    for (m <- pure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => pure")
                    }
                    for (m <- dPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific pure")
                    }
                    for (m <- sideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => side-effect free")
                    }
                    for (m <- dSideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific side-effect free")
                    }
                    for (m <- externallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => externally pure")
                    }
                    for (m <- dExternallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific externally pure")
                    }
                    for (m <- externallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => externally side-effect free")
                    }
                    for (m <- dExternallySideEffectFree) {
                        resultsWriter.println(
                            s"${m.definedMethod.toJava} => domain-specific externally side-effect free"
                        )
                    }
                    for ((m, p) <- contextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => contextually pure: $p")
                    }
                    for ((m, p) <- dContextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific contextually pure: $p")
                    }
                    for ((m, p) <- contextuallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => contextually side-effect free: $p")
                    }
                    for ((m, p) <- dContextuallySideEffectFree) {
                        resultsWriter.println(
                            s"${m.definedMethod.toJava} => domain-specific contextually side-effect free: $p"
                        )
                    }
                    for (m <- lbImpure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => impure")
                    }
                }
            } finally {
                if (resultsWriter != null) resultsWriter.close()
            }
        } else {
            val result =
                ps.toString(false) +
                    "\ncompile-time pure:                     " + compileTimePure.size +
                    "\nAt least pure:                         " + pure.size +
                    "\nAt least domain-specficic pure:        " + dPure.size +
                    "\nAt least side-effect free:             " + sideEffectFree.size +
                    "\nAt least d-s side effect free:         " + dSideEffectFree.size +
                    "\nAt least externally pure:              " + externallyPure.size +
                    "\nAt least d-s externally pure:          " + dExternallyPure.size +
                    "\nAt least externally side-effect free:  " + externallySideEffectFree.size +
                    "\nAt least d-s ext. side-effect free:    " + dExternallySideEffectFree.size +
                    "\nAt least contextually pure:            " + contextuallyPure.size +
                    "\nAt least d-s contextually pure:        " + dContextuallyPure.size +
                    "\nAt least contextually side-effect free:" + contextuallySideEffectFree.size +
                    "\nAt least d-s cont. side-effect free:   " + dContextuallySideEffectFree.size +
                    "\nImpure:                                " + lbImpure.size +
                    "\nTotal:                                 " + projectEntitiesWithPurity.size
            Console.println(result)
            Console.println(s"Call-graph time: $callGraphTime")
            Console.println(s"Analysis time: $analysisTime")
        }
    }

    def main(args: Array[String]): Unit = {

        val analysisConfig = new PurityConfig(args)

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        time {
            if (analysisConfig(MultiProjectsCommand)) {
                for (subp <- analysisConfig(ClassPathCommand).flatMap(_.listFiles).filter(_.isDirectory)) {
                    println(s"${subp.getName}: ${Calendar.getInstance().getTime}")
                    evaluate(analysisConfig, subp)
                }
            } else {
                val cp = analysisConfig.get(JDKCommand).getOrElse(analysisConfig(ClassPathCommand).head)
                evaluate(analysisConfig, cp)
            }
        }(t => println("evaluation time: " + t.toSeconds))

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }
}
