/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Calendar
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.Commandline_base.commandlines.{AnalysisCommand, AnalysisNameCommand, CallGraphCommand, ClassPathCommand, CloseWorldCommand, DebugCommand, DomainCommand, EagerCommand, EscapeCommand, EvalDirCommand, FieldAssignabilityCommand, IndividualCommand, JDKCommand, LibraryCommand, LibraryDirectoryCommand, MultiProjectsCommand, OpalConf, PackagesCommand, ProjectDirectoryCommand, RaterCommand, SchedulingStrategyCommand, ThreadsNumCommand}
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
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
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.log.LogContext
import org.opalj.support.parser.{AnalysisCommandExternalParser, CallGraphCommandExternalParser, ClassPathCommandExternalParser, DomainCommandExternalParser, RaterCommandExternalParser}
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.DomainSpecificRater
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.rogach.scallop.ScallopConf

/**
 * `PurityConf` is a configuration class for parsing and managing command-line arguments related to purity analysis
 * in the OPAL framework. It extends `ScallopConf` and mixes in `OpalConf`, providing structured options for various
 * analysis settings, such as paths, analysis schedulers, and specific analysis configurations.
 */

class PurityConf(args: Array[String]) extends ScallopConf(args) with OpalConf {

    // Commands
    private val classPathCommand = getPlainScallopOption(ClassPathCommand)
    private val projectDirCommand = getPlainScallopOption(ProjectDirectoryCommand)
    private val libDirCommand = getPlainScallopOption(LibraryDirectoryCommand)
    private val analysisCommand = getChoiceScallopOption(AnalysisCommand)
    private val fieldAssignabilityCommand = getChoiceScallopOption(FieldAssignabilityCommand)
    private val escapeCommand = getChoiceScallopOption(EscapeCommand)
    private val eagerCommand = getPlainScallopOption(EagerCommand)
    private val domainCommand = getPlainScallopOption(DomainCommand)
    private val raterCommand = getPlainScallopOption(RaterCommand)
    private val callGraphCommand = getPlainScallopOption(CallGraphCommand)
    private val jdkCommand = getPlainScallopOption(JDKCommand)
    private val individualCommand = getPlainScallopOption(IndividualCommand)
    private val closedWorldCommand = getPlainScallopOption(CloseWorldCommand)
    private val libraryCommand = getPlainScallopOption(LibraryCommand)
    private val debugCommand = getPlainScallopOption(DebugCommand)
    private val multiProjectsCommand = getPlainScallopOption(MultiProjectsCommand)
    private val evaluationDirCommand = getPlainScallopOption(EvalDirCommand)
    private val packagesCommand = getPlainScallopOption(PackagesCommand)
    private val threadsNumCommand = getPlainScallopOption(ThreadsNumCommand)
    private val analysisNameCommand = getPlainScallopOption(AnalysisNameCommand)
    private val schedulingStrategyCommand = getPlainScallopOption(SchedulingStrategyCommand)

    verify()

    // Parsed data
    var classPathFiles = parseCommandWithExternalParser(classPathCommand, ClassPathCommandExternalParser).getOrElse(null)
    var projectDirectory = parseCommandWithInternalParser(projectDirCommand, ProjectDirectoryCommand).getOrElse(null)
    var libraryDirectory = parseCommandWithInternalParser(libDirCommand, LibraryDirectoryCommand).getOrElse(null)
    var analysisScheduler = parseCommandWithExternalParser(analysisCommand, AnalysisCommandExternalParser).getOrElse(null)
    var support: Option[List[FPCFAnalysisScheduler]] = None

    if (fieldAssignabilityCommand.isDefined && escapeCommand.isDefined && eagerCommand.isDefined && analysisScheduler != null) support = Some(parseArgumentsForSupport(
            analysisCommand.apply(),
            fieldAssignabilityCommand.apply(),
            escapeCommand.apply(),
            eagerCommand.apply(),
            analysisScheduler
        )) else support = None

    var domain = parseCommandWithExternalParser(domainCommand, DomainCommandExternalParser).getOrElse(null)
    var rater = parseCommandWithExternalParser(raterCommand, RaterCommandExternalParser).getOrElse(null)
    var callGraph = parseCommandWithExternalParser(callGraphCommand, CallGraphCommandExternalParser).getOrElse(null)
    var jdk: Boolean = parseCommand(jdkCommand).getOrElse(false)
    var individual: Boolean = parseCommand(individualCommand).getOrElse(false)
    var closedWorld:Boolean = parseCommand(closedWorldCommand).getOrElse(false)
    var library: Boolean = parseCommand(libraryCommand).getOrElse(false)
    var debug: Boolean = parseCommand(debugCommand).getOrElse(false)
    var multiProjects: Boolean = parseCommand(multiProjectsCommand).getOrElse(false)
    var evaluationDir = parseCommandWithInternalParser(evaluationDirCommand, EvalDirCommand).getOrElse(null)
    var packages = parseCommandWithInternalParser(packagesCommand, PackagesCommand).getOrElse(null)
    var threadsNum: Int = parseCommand(threadsNumCommand).getOrElse(0)
    var configurationName = parseCommand(analysisNameCommand).getOrElse(null)
    var schedulingStrategy = parseCommand(schedulingStrategyCommand).getOrElse(null)


    private def parseArgumentsForSupport(analysis: String, fieldAssignability: String, escape: String, eager: Boolean, analysisScheduler: Any) = {
        var support = Nil

        if(analysis == "L2") support = List(
                LazyFieldImmutabilityAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )

        if (eager) {
            support ::= EagerClassImmutabilityAnalysis
            support ::= EagerTypeImmutabilityAnalysis
        } else {
            support ::= LazyClassImmutabilityAnalysis
            support ::= LazyTypeImmutabilityAnalysis
        }

        escape match {
            case "L0" =>
                support ::= LazySimpleEscapeAnalysis

            case null | "L1" =>
                support ::= LazyInterProceduralEscapeAnalysis

            case "none" =>

            case _ =>
                Console.println(s"unknown escape analysis: $escape")
                printHelp()
        }

        fieldAssignability match {
            case "L0" if eager => support ::= EagerL0FieldAssignabilityAnalysis

            case "L0" => support ::= LazyL0FieldAssignabilityAnalysis

            case "L1" if eager => support ::= EagerL1FieldAssignabilityAnalysis

            case "L1" => support ::= LazyL1FieldAssignabilityAnalysis

            case "L2" if eager =>
                support ::= EagerL2FieldAssignabilityAnalysis

            case "L2" =>
                support ::= LazyL2FieldAssignabilityAnalysis

            case "none" =>

            case null => analysisScheduler match {
                case LazyL0PurityAnalysis => support ::= LazyL0FieldAssignabilityAnalysis
                case LazyL1PurityAnalysis => support ::= LazyL1FieldAssignabilityAnalysis
                case LazyL2PurityAnalysis => support ::= LazyL1FieldAssignabilityAnalysis
            }

            case _ =>
                Console.println(s"unknown field assignability analysis: $fieldAssignability")
                printHelp()
        }

        support
    }
}


/**
 * Executes a purity analysis (L2 by default) along with necessary supporting analysis.
 *
 * @author Dominik Helm
 */
object Purity {

    val JDKPackages = List(
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
        cp:                    File,
        projectDir:            Option[String],
        libDir:                Option[String],
        analysis:              FPCFLazyAnalysisScheduler,
        support:               List[FPCFAnalysisScheduler],
        domain:                Class[_ <: Domain with RecordDefUse],
        configurationName:     Option[String],
        schedulingStrategy:    Option[String],
        rater:                 DomainSpecificRater,
        callGraphKey:          CallGraphKey,
        jdk:            Boolean,
        individual:            Boolean,
        numThreads:            Int,
        closedWorldAssumption: Boolean,
        isLibrary:             Boolean,
        debug:                 Boolean,
        evaluationDir:         Option[File],
        packages:              Option[Array[String]]
    ): Unit = {
        val classFiles = projectDir match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = libDir match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => Iterable.empty
        }

        val JDKFiles = if (!jdk) Iterable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        val isJDK: Boolean = cp eq JRELibraryFolder
        val dirName = if (isJDK) "JDK" else cp.getName
        val projectEvalDir = evaluationDir.map(new File(_, dirName))
        if (projectEvalDir.isDefined && !projectEvalDir.get.exists()) projectEvalDir.get.mkdir()

        var projectTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        // TODO: use variables for the constants
        implicit var config: Config =
            if (isLibrary)
                ConfigFactory.load("LibraryProject.conf")
            else
                ConfigFactory.load("CommandLineProject.conf")

        // TODO: in case of application this value is already set
        if (closedWorldAssumption) {
            config = config.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
        }

        if (schedulingStrategy.isDefined) {
            config = config.withValue(
                PKESequentialPropertyStore.TasksManagerKey,
                ConfigValueFactory.fromAnyRef(schedulingStrategy.get)
            )
        }

        val project = time {
            Project(
                classFiles,
                libFiles ++ JDKFiles,
                libraryClassFilesAreInterfacesOnly = false,
                Iterable.empty
            )
        } { t => projectTime = t.toSeconds }

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = numThreads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )

        PropertyStore.updateDebug(debug)
        val ps = time { project.get(PropertyStoreKey) } { t => propertyStoreTime = t.toSeconds }

        analysis match {
            case LazyL0PurityAnalysis =>
            case LazyL1PurityAnalysis => L1PurityAnalysis.setRater(Some(rater))
            case LazyL2PurityAnalysis => L2PurityAnalysis.setRater(Some(rater))
        }

        val declaredMethods = project.get(DeclaredMethodsKey)

        val allMethods: Iterable[DefinedMethod] =
            for (cf <- project.allProjectClassFiles; m <- cf.methodsWithBody)
                yield declaredMethods(m)

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
            project.get(callGraphKey)
        } { t => callGraphTime = t.toSeconds }

        val reachableMethods =
            ps.entities(Callers.key).collect {
                case FinalEP(m: DeclaredMethod, c: Callers) if c ne NoCallers => m
            }.toSet

        val contextProvider = project.get(ContextProviderKey)
        val analyzedContexts = projMethods.filter(reachableMethods.contains).map(contextProvider.newContext(_))

        time {
            val analyses = analysis :: support

            manager.runAll(
                analyses,
                (css: List[ComputationSpecification[FPCFAnalysis]]) =>
                    if (css.contains(analysis)) {
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
            val pn = ep.e.asInstanceOf[Context].method.declaringClassType.asObjectType.packageName
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
                    s"${configurationName.get};${projectTime.toString(false)};" +
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

            val results = new File(projectEvalDir.get, "method-results.csv")
            val resultsNew = !results.exists()
            val resultsWriter = new PrintWriter(new FileOutputStream(results, !individual))
            try {
                if (resultsNew) {
                    results.createNewFile()
                    if (!individual)
                        resultsWriter.println("analysisName;compile time pure;pure;" +
                            "domain-specific pure;side-effect free;" +
                            "domain-specific side-effect free;externally pure;" +
                            "domain-specific externally pure;externally side-effect free;" +
                            "domain-specific externally side-effect free;" +
                            "contextually pure;domain-specific contextually pure;" +
                            "contextually side-effect free;domain-specific contextually " +
                            "side-effect free;impure;count")
                }

                if (!individual) {
                    resultsWriter.println(
                        s"${configurationName.get};${compileTimePure.size};${pure.size};${dPure.size};" +
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

        val purityConf = new PurityConf(args)

        if(args.contains("--help") || args.contains("-h")) {
            return
        }

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        time {
            if (purityConf.multiProjects) {
                for (subp <- purityConf.classPathFiles.head.listFiles().filter(_.isDirectory)) {
                    println(s"${subp.getName}: ${Calendar.getInstance().getTime}")
                    evaluate(
                        subp,
                        purityConf.projectDirectory,
                        purityConf.libraryDirectory,
                        purityConf.analysisScheduler,
                        purityConf.support.getOrElse(List()),
                        purityConf.domain,
                        Option(purityConf.configurationName),
                        Option(purityConf.schedulingStrategy),
                        purityConf.rater,
                        purityConf.callGraph,
                        purityConf.jdk,
                        purityConf.individual,
                        purityConf.threadsNum,
                        purityConf.closedWorld,
                        purityConf.library || (subp eq JRELibraryFolder),
                        purityConf.debug,
                        purityConf.evaluationDir,
                        purityConf.packages
                    )
                }
            } else {
                evaluate(
                    purityConf.classPathFiles.head,
                    purityConf.projectDirectory,
                    purityConf.libraryDirectory,
                    purityConf.analysisScheduler,
                    purityConf.support.getOrElse(List()),
                    purityConf.domain,
                    Option(purityConf.configurationName),
                    Option(purityConf.schedulingStrategy),
                    purityConf.rater,
                    purityConf.callGraph,
                    purityConf.jdk,
                    purityConf.individual,
                    purityConf.threadsNum,
                    purityConf.closedWorld,
                    purityConf.library || (purityConf.classPathFiles.head eq JRELibraryFolder),
                    purityConf.debug,
                    purityConf.evaluationDir,
                    purityConf.packages
                )
            }
        }(t => println("evaluation time: " + t.toSeconds))

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }
}
