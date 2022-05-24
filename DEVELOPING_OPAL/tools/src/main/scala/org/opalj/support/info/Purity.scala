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

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.CompileTimePure
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
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.ai.Domain
import org.opalj.ai.domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.fpcf.analyses.EagerUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.log.LogContext
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.DomainSpecificRater
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis

/**
 * Executes a purity analysis (L2 by default) along with necessary supporting analysis.
 *
 * @author Dominik Helm
 */
object Purity {

    //OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)

    def usage: String = {
        "Usage: java …PurityAnalysisEvaluation \n"+
            "-cp <JAR file/Folder containing class files> OR -JDK\n"+
            "[-projectDir <directory with project class files relative to cp>]\n"+
            "[-libDir <directory with library class files relative to cp>]\n"+
            "[-analysis <L0|L1|L2> (Default: L2, the most precise analysis configuration)]\n"+
            "[-fieldMutability <none|L0|L1|L2> (Default: Depends on analysis level)]\n"+
            "[-escape <none|L0|L1> (Default: L1, the most precise configuration)]\n"+
            "[-domain <class name of the abstract interpretation domain>]\n"+
            "[-rater <class name of the rater for domain-specific actions>]\n"+
            "[-callGraph <CHA|RTA|PointsTo> (Default: RTA)]\n"+
            "[-eager] (supporting analyses are executed eagerly)\n"+
            "[-noJDK] (do not analyze any JDK methods)\n"+
            "[-individual] (reports the purity result for each method)\n"+
            "[-closedWorld] (uses closed world assumption, i.e. no class can be extended)\n"+
            "[-library] (assumes that the target is a library)\n"+
            "[-debug] (enable debug output from PropertyStore)\n"+
            "[-multi] (analyzes multiple projects in the subdirectories of -cp)\n"+
            "[-eval <path to evaluation directory>]\n"+
            "[-packages <colon separated list of packages, e.g. java/util:javax>]\n"+
            "[-j <number of threads to be used> (0 for the sequential implementation)]\n"+
            "[-analysisName <analysisName which defines the analysis within the results file>]\n"+
            "[-schedulingStrategy <schedulingStrategy which defines the analysis within the results file>]\n"+
            "Example:\n\tjava …PurityAnalysisEvaluation -JDK -individual -closedWorld"
    }

    val JDKPackages = List("java/", "javax", "javafx", "jdk", "sun", "oracle", "com/sun",
        "netscape", "org/ietf/jgss", "org/jcp/xml/dsig/internal", "org/omg", "org/w3c/dom",
        "org/xml/sax")

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
        withoutJDK:            Boolean,
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

        val JDKFiles = if (withoutJDK) Iterable.empty
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
        implicit var config: Config = if (isLibrary)
            ConfigFactory.load("LibraryProject.conf")
        else
            ConfigFactory.load("ApplicationProject.conf")

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

        val analyzedMethods = projMethods.filter(reachableMethods.contains)

        time {
            val analyses = analysis :: support

            manager.runAll(
                analyses,
                { css: List[ComputationSpecification[FPCFAnalysis]] =>
                    if (css.contains(analysis)) {
                        analyzedMethods.foreach { dm => ps.force(dm, br.fpcf.properties.Purity.key) }
                    }
                }
            )

        } { t => analysisTime = t.toSeconds }
        ps.shutdown()

        val entitiesWithPurity = ps(analyzedMethods, br.fpcf.properties.Purity.key).filter {
            case FinalP(p) => p ne ImpureByLackOfInformation
            case ep        => throw new RuntimeException(s"non final purity result $ep")
        }

        val projectEntitiesWithPurity = entitiesWithPurity.filter { ep =>
            val pn = ep.e.asInstanceOf[DeclaredMethod].declaringClassType.asObjectType.packageName
            packages match {
                case None     => isJDK || !JDKPackages.exists(pn.startsWith)
                case Some(ps) => ps.exists(pn.startsWith)
            }
        }.toSeq

        def isExternal(dm: DefinedMethod, p: IntTrieSet): Boolean = {
            !dm.definedMethod.isStatic && p.size == 1 && p.head == 0
        }

        val compileTimePure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, CompileTimePure) => m }
        val pure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, Pure) => m }
        val sideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, SideEffectFree) => m }
        val externallyPure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, ContextuallyPure(p)) if isExternal(m, p) => m }
        val externallySideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, ContextuallySideEffectFree(p)) if isExternal(m, p) => m }
        val contextuallyPure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, ContextuallyPure(p)) if !isExternal(m, p) => (m, p) }
        val contextuallySideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, ContextuallySideEffectFree(p)) if !isExternal(m, p) => (m, p) }
        val dPure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DPure) => m }
        val dSideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DSideEffectFree) => m }
        val dExternallyPure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DContextuallyPure(p)) if isExternal(m, p) => m }
        val dExternallySideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DContextuallySideEffectFree(p)) if isExternal(m, p) => m }
        val dContextuallyPure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DContextuallyPure(p)) if !isExternal(m, p) => (m, p) }
        val dContextuallySideEffectFree = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, DContextuallySideEffectFree(p)) if !isExternal(m, p) => (m, p) }
        val lbImpure = projectEntitiesWithPurity.collect { case FinalEP(m: DefinedMethod, ImpureByAnalysis) => m }

        if (projectEvalDir.isDefined) {

            // WRITE ANALYSIS OUTPUT

            val output = new File(projectEvalDir.get, "purityResults.csv")
            val newFile = !output.exists()
            val outputWriter = new PrintWriter(new FileOutputStream(output, true))
            try {
                if (newFile) {
                    output.createNewFile()
                    outputWriter.println(
                        "analysisName;project time;propertyStore time;"+
                            "callGraph time;analysis time; total time;"+
                            "compile time pure;pure;domain-specific pure;"+
                            "side-effect free;domain-specific side-effect free;"+
                            "externally pure;domain-specific externally pure;"+
                            "externally side-effect free; domain-specific externally side-effect "+
                            "free;contextually pure;domain-specific contextually pure;"+
                            "contextually side-effect free;domain-specific contextually "+
                            "side-effect free;impure;count"
                    )
                }
                val totalTime = projectTime + propertyStoreTime + callGraphTime + analysisTime
                outputWriter.println(
                    s"${configurationName.get};${projectTime.toString(false)};"+
                        s"${propertyStoreTime.toString(false)};"+
                        s"${callGraphTime.toString(false)};"+
                        s"${analysisTime.toString(false)};"+
                        s"${totalTime.toString(false)};"+
                        s"${compileTimePure.size};${pure.size};${dPure.size};"+
                        s"${sideEffectFree.size};${dSideEffectFree.size};"+
                        s"${externallyPure.size};${dExternallyPure.size};"+
                        s"${contextuallyPure.size};${dContextuallyPure.size};"+
                        s"${externallySideEffectFree.size};"+
                        s"${dExternallySideEffectFree.size};"+
                        s"${contextuallySideEffectFree.size};"+
                        s"${dContextuallySideEffectFree.size};"+
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
                        resultsWriter.println("analysisName;compile time pure;pure;"+
                            "domain-specific pure;side-effect free;"+
                            "domain-specific side-effect free;externally pure;"+
                            "domain-specific externally pure;externally side-effect free;"+
                            "domain-specific externally side-effect free;"+
                            "contextually pure;domain-specific contextually pure;"+
                            "contextually side-effect free;domain-specific contextually "+
                            "side-effect free;impure;count")
                }

                if (!individual) {
                    resultsWriter.println(
                        s"${configurationName.get};${compileTimePure.size};${pure.size};${dPure.size};"+
                            s"${sideEffectFree.size};${dSideEffectFree.size};"+
                            s"${externallyPure.size};${dExternallyPure.size};"+
                            s"${contextuallyPure.size};${dContextuallyPure.size};"+
                            s"${externallySideEffectFree.size};"+
                            s"${dExternallySideEffectFree.size};"+
                            s"${contextuallySideEffectFree.size};"+
                            s"${dContextuallySideEffectFree.size};"+
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
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific externally side-effect free")
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
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific contextually side-effect free: $p")
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
                    "\nTotal:                                 "+projectEntitiesWithPurity.size
            Console.println(result)
            Console.println(s"Call-graph time: $callGraphTime")
            Console.println(s"Analysis time: $analysisTime")
        }
    }

    def main(args: Array[String]): Unit = {

        // Parameters:
        var cp: File = null
        var projectDir: Option[String] = None
        var libDir: Option[String] = None
        var analysisName: Option[String] = None
        var fieldMutabilityAnalysisName: Option[String] = None
        var escapeAnalysisName: Option[String] = None
        var domainName: Option[String] = None
        var raterName: Option[String] = None
        var callGraphName: Option[String] = None
        var configurationName: Option[String] = None
        var schedulingStrategy: Option[String] = None
        var withoutJDK = false
        var individual = false
        var isLibrary = false
        var cwa = false
        var debug = false
        var multiProjects = false
        var eager = false
        var evaluationDir: Option[File] = None
        var packages: Option[Array[String]] = None
        var numThreads = PropertyStoreKey.parallelismLevel

        // PARSING PARAMETERS
        var i = 0

        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        while (i < args.length) {
            args(i) match {
                case "-cp"                 => cp = new File(readNextArg())
                case "-projectDir"         => projectDir = Some(readNextArg())
                case "-libDir"             => libDir = Some(readNextArg())
                case "-analysis"           => analysisName = Some(readNextArg())
                case "-fieldMutability"    => fieldMutabilityAnalysisName = Some(readNextArg())
                case "-escape"             => escapeAnalysisName = Some(readNextArg())
                case "-domain"             => domainName = Some(readNextArg())
                case "-rater"              => raterName = Some(readNextArg())
                case "-callGraph"          => callGraphName = Some(readNextArg())
                case "-analysisName"       => configurationName = Some(readNextArg())
                case "-schedulingStrategy" => schedulingStrategy = Some(readNextArg())
                case "-eager"              => eager = true
                case "-individual"         => individual = true
                case "-closedWorld"        => cwa = true
                case "-library"            => isLibrary = true
                case "-debug"              => debug = true
                case "-multi"              => multiProjects = true
                case "-eval"               => evaluationDir = Some(new File(readNextArg()))
                case "-packages"           => packages = Some(readNextArg().split(':'))
                case "-j"                  => numThreads = readNextArg().toInt
                case "-noJDK"              => withoutJDK = true
                case "-JDK" =>
                    cp = JRELibraryFolder; withoutJDK = true

                case unknown =>
                    Console.println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }

        if (configurationName.isEmpty) {
            configurationName = Some(s"RUN-${Calendar.getInstance().getTime.toString}")
        }

        if (cp eq null) {
            Console.println("no classpath given (use -cp <classpath> or -JDK)")
            Console.println(usage)
            return ;
        }

        var support: List[FPCFAnalysisScheduler] = Nil
        val analysis: FPCFLazyAnalysisScheduler = analysisName match {
            case Some("L0") => LazyL0PurityAnalysis

            case Some("L1") => LazyL1PurityAnalysis

            case None | Some("L2") =>
                support = List(
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                LazyL2PurityAnalysis

            case Some(a) =>
                Console.println(s"unknown analysis: $a")
                Console.println(usage)
                return ;
        }

        if (eager) {
            support ::= EagerClassImmutabilityAnalysis
            support ::= EagerTypeImmutabilityAnalysis
        } else {
            support ::= LazyClassImmutabilityAnalysis
            support ::= LazyTypeImmutabilityAnalysis
        }

        escapeAnalysisName match {
            case Some("L0") =>
                support ::= LazySimpleEscapeAnalysis

            case None | Some("L1") =>
                support ::= LazyInterProceduralEscapeAnalysis

            case Some("none") =>

            case Some(a) =>
                Console.println(s"unknown escape analysis: $a")
                Console.println(usage)
                return ;
        }

        fieldMutabilityAnalysisName match {
            case Some("L0") if eager => support ::= EagerL0FieldMutabilityAnalysis

            case Some("L0")          => support ::= LazyL0FieldMutabilityAnalysis

            case Some("L1") if eager => support ::= EagerL1FieldMutabilityAnalysis

            case Some("L1")          => support ::= LazyL1FieldMutabilityAnalysis

            case Some("L2") if eager =>
                support ::= EagerL2FieldMutabilityAnalysis
                support ::= EagerUnsoundPrematurelyReadFieldsAnalysis

            case Some("L2") =>
                support ::= LazyL2FieldMutabilityAnalysis
                support ::= LazyUnsoundPrematurelyReadFieldsAnalysis

            case Some("none") =>

            case None => analysis match {
                case LazyL0PurityAnalysis => LazyL0FieldMutabilityAnalysis
                case LazyL1PurityAnalysis => LazyL1FieldMutabilityAnalysis
                case LazyL2PurityAnalysis => LazyL1FieldMutabilityAnalysis
            }

            case Some(a) =>
                Console.println(s"unknown field mutability analysis: $a")
                Console.println(usage)
                return ;
        }

        val d =
            if (domainName.isEmpty)
                classOf[domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
            else {
                Class.forName(domainName.get).asInstanceOf[Class[Domain with RecordDefUse]]
            }

        val rater = if (raterName.isEmpty) {
            SystemOutLoggingAllExceptionRater
        } else {
            import scala.reflect.runtime.universe.runtimeMirror
            val mirror = runtimeMirror(getClass.getClassLoader)
            val module = mirror.staticModule(raterName.get)
            mirror.reflectModule(module).instance.asInstanceOf[DomainSpecificRater]
        }

        val callGraphKey = callGraphName match {
            case Some("CHA")        => CHACallGraphKey
            case Some("PointsTo")   => AllocationSiteBasedPointsToCallGraphKey
            case Some("RTA") | None => RTACallGraphKey
            case Some(a) =>
                Console.println(s"unknown call graph analysis: $a")
                Console.println(usage)
                return ;
        }

        if (evaluationDir.isDefined && !evaluationDir.get.exists()) evaluationDir.get.mkdir

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        time {
            if (multiProjects) {
                for (subp <- cp.listFiles().filter(_.isDirectory)) {
                    println(s"${subp.getName}: ${Calendar.getInstance().getTime}")
                    evaluate(
                        subp,
                        projectDir,
                        libDir,
                        analysis,
                        support,
                        d,
                        configurationName,
                        schedulingStrategy,
                        rater,
                        callGraphKey,
                        withoutJDK,
                        individual,
                        numThreads,
                        cwa,
                        isLibrary || (subp eq JRELibraryFolder),
                        debug,
                        evaluationDir,
                        packages
                    )
                }
            } else {
                evaluate(
                    cp,
                    projectDir,
                    libDir,
                    analysis,
                    support,
                    d,
                    configurationName,
                    schedulingStrategy,
                    rater,
                    callGraphKey,
                    withoutJDK,
                    individual,
                    numThreads,
                    cwa,
                    isLibrary || (cp eq JRELibraryFolder),
                    debug,
                    evaluationDir,
                    packages
                )
            }
        }(t => println("evaluation time: "+t.toSeconds))

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }
}
