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
import org.opalj.collection.immutable.Chain
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
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.fpcf.cg.properties.NoCallers
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
import org.opalj.ai.Domain
import org.opalj.ai.domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.DomainSpecificRater
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Executes a purity analysis (L2 by default) along with necessary supporting analysis.
 *
 * @author Dominik Helm
 */
object Purity {

    def usage: String = {
        "Usage: java …PurityAnalysisEvaluation \n"+
            "-cp <JAR file/Folder containing class files> OR -JDK\n"+
            "[-projectDir <directory with project class files relative to cp>]\n"+
            "[-libDir <directory with library class files relative to cp>]\n"+
            "[-analysis <L0|L1|L2> (Default: L2, the most precise analysis configuration)]\n"+
            "[-domain <class name of the abstract interpretation domain>]\n"+
            "[-rater <class name of the rater for domain-specific actions>]\n"+
            "[-noJDK] (do not analyze any JDK methods)\n"+
            "[-individual] (reports the purity result for each method)\n"+
            "[-closedWorld] (uses closed world assumption, i.e. no class can be extended)\n"+
            "[-debug] (enable debug output from PropertyStore)\n"+
            "[-multi] (analyzes multiple projects in the subdirectories of -cp)\n"+
            "[-eval <path to evaluation directory>]\n"+
            "Example:\n\tjava …PurityAnalysisEvaluation -JDK -individual -closedWorld"
    }

    val supportingAnalyses = IndexedSeq(
        List[FPCFAnalysisScheduler](
            LazyL0FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        ),
        List[FPCFAnalysisScheduler](
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        ),
        List[FPCFAnalysisScheduler](
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        )
    )

    def evaluate(
        cp:                    File,
        projectDir:            Option[String],
        libDir:                Option[String],
        analysis:              FPCFLazyAnalysisScheduler,
        domain:                Class[_ <: Domain with RecordDefUse],
        rater:                 DomainSpecificRater,
        withoutJDK:            Boolean,
        individual:            Boolean,
        closedWorldAssumption: Boolean,
        debug:                 Boolean,
        evaluationDir:         Option[File]
    ): Unit = {
        val classFiles = projectDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = libDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ Traversable.empty
        }

        val JDKFiles = if (withoutJDK) Traversable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        val dirName = if (cp eq JRELibraryFolder) "JDK" else cp.getName
        val projectEvalDir = evaluationDir.map(new File(_, dirName))
        if (projectEvalDir.isDefined && !projectEvalDir.get.exists()) projectEvalDir.get.mkdir()

        val isLibrary = cp eq JRELibraryFolder // TODO make configurable

        var projectTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        // todo: use variables for the constants
        val baseConfig = if (isLibrary)
            ConfigFactory.load("LibraryProject.conf")
        else
            ConfigFactory.load("ApplicationProject.conf")

        // todo in case of application this value is already set
        implicit val config: Config =
            if (closedWorldAssumption) baseConfig.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
            else baseConfig

        val project = time {
            Project(
                classFiles,
                libFiles ++ JDKFiles,
                libraryClassFilesAreInterfacesOnly = false,
                Traversable.empty
            )
        } { t ⇒ projectTime = t.toSeconds }

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               ⇒ Set(domain)
            case Some(requirements) ⇒ requirements + domain
        }

        PropertyStore.updateDebug(debug)
        val ps = time { project.get(PropertyStoreKey) } { t ⇒ propertyStoreTime = t.toSeconds }

        analysis match {
            case LazyL0PurityAnalysis ⇒
            case LazyL1PurityAnalysis ⇒ L1PurityAnalysis.setRater(Some(rater))
            case LazyL2PurityAnalysis ⇒ L2PurityAnalysis.setRater(Some(rater))
        }

        val support = analysis match {
            case LazyL0PurityAnalysis ⇒ supportingAnalyses(0)
            case LazyL1PurityAnalysis ⇒ supportingAnalyses(1)
            case LazyL2PurityAnalysis ⇒ supportingAnalyses(2)
        }

        val declaredMethods = project.get(DeclaredMethodsKey)

        val projMethods: Seq[DefinedMethod] =
            for (cf ← project.allProjectClassFiles; m ← cf.methodsWithBody)
                yield declaredMethods(m)

        val manager = project.get(FPCFAnalysesManagerKey)

        time {
            project.get(RTACallGraphKey)
        } { t ⇒ callGraphTime = t.toSeconds }

        val reachableMethods =
            ps.entities(Callers.key).collect {
                case FinalEP(e: DeclaredMethod, c: Callers) if c ne NoCallers ⇒ e
            }.toSet

        val analyzedMethods = projMethods.filter(reachableMethods.contains)

        time {
            val analyses = analysis :: support

            manager.runAll(
                analyses,
                { css: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                    if (css.contains(analysis)) {
                        analyzedMethods.foreach { dm ⇒ ps.force(dm, br.fpcf.properties.Purity.key) }
                    }
                }
            )

        } { t ⇒ analysisTime = t.toSeconds }
        ps.shutdown()

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
            val resultsWriter = new PrintWriter(new FileOutputStream(results, !individual))
            try {
                if (resultsNew) {
                    results.createNewFile()
                    if (!individual)
                        resultsWriter.println("compile time pure;pure;domain-specific pure;"+
                            "side-effect free;domain-specific side-effect free;"+
                            "externally pure;domain-specific externally pure;"+
                            "externally side-effect free; domain-specific externally side-effect "+
                            "free;contextually pure;domain-specific contextually pure;"+
                            "contextually side-effect free;domain-specific contextually "+
                            "side-effect free;impure;count")
                }

                if (!individual) {
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
                } else {
                    for (m ← compileTimePure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => compile time pure")
                    }
                    for (m ← pure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => pure")
                    }
                    for (m ← dPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific pure")
                    }
                    for (m ← sideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => side-effect free")
                    }
                    for (m ← dSideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific side-effect free")
                    }
                    for (m ← externallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => externally pure")
                    }
                    for (m ← dExternallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific externally pure")
                    }
                    for (m ← externallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => externally side-effect free")
                    }
                    for (m ← dExternallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific externally side-effect free")
                    }
                    for ((m, p) ← contextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => contextually pure: $p")
                    }
                    for ((m, p) ← dContextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific contextually pure: $p")
                    }
                    for ((m, p) ← contextuallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => contextually side-effect free: $p")
                    }
                    for ((m, p) ← dContextuallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => domain-specific contextually side-effect free: $p")
                    }
                    for (m ← lbImpure) {
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
                    "\nTotal:                                 "+purityEs.size
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
        var domainName: Option[String] = None
        var raterName: Option[String] = None
        var withoutJDK = false
        var individual = false
        var cwa = false
        var debug = false
        var multiProjects = false
        var evaluationDir: Option[File] = None

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
                case "-cp"          ⇒ cp = new File(readNextArg())
                case "-projectDir"  ⇒ projectDir = Some(readNextArg())
                case "-libDir"      ⇒ libDir = Some(readNextArg())
                case "-analysis"    ⇒ analysisName = Some(readNextArg())
                case "-domain"      ⇒ domainName = Some(readNextArg())
                case "-rater"       ⇒ raterName = Some(readNextArg())
                case "-individual"  ⇒ individual = true
                case "-closedWorld" ⇒ cwa = true
                case "-debug"       ⇒ debug = true
                case "-multi"       ⇒ multiProjects = true
                case "-eval"        ⇒ evaluationDir = Some(new File(readNextArg()))
                case "-noJDK"       ⇒ withoutJDK = true
                case "-JDK" ⇒
                    cp = JRELibraryFolder; withoutJDK = true

                case unknown ⇒
                    Console.println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }

        if (cp eq null) {
            Console.println("no classpath given (use -cp <classpath> or -JDK)")
            Console.println(usage)
            return ;
        }

        val analysis: FPCFLazyAnalysisScheduler = analysisName match {
            case Some("L0")        ⇒ LazyL0PurityAnalysis
            case Some("L1")        ⇒ LazyL1PurityAnalysis
            case None | Some("L2") ⇒ LazyL2PurityAnalysis

            case Some(a) ⇒
                Console.println(s"unknown analysis: $a")
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

        if (evaluationDir.isDefined && !evaluationDir.get.exists()) evaluationDir.get.mkdir

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        time {
            if (multiProjects) {
                for (subp ← cp.listFiles().filter(_.isDirectory)) {
                    println(s"${subp.getName}: ${Calendar.getInstance().getTime}")
                    evaluate(
                        subp,
                        projectDir,
                        libDir,
                        analysis,
                        d,
                        rater,
                        withoutJDK,
                        individual,
                        cwa,
                        debug,
                        evaluationDir
                    )
                }
            } else {
                evaluate(
                    cp,
                    projectDir,
                    libDir,
                    analysis,
                    d,
                    rater,
                    withoutJDK,
                    individual,
                    cwa,
                    debug,
                    evaluationDir
                )
            }
        }(t ⇒ println("evaluation time: "+t.toSeconds))

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }
}
