/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package support
package info

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Calendar

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.Config
import org.opalj.ai.domain
import org.opalj.ai.Domain
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.analyses.purity.L1PurityAnalysis
import org.opalj.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL0PurityAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.purity.LazyL1PurityAnalysis
import org.opalj.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.fpcf.analyses.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.fpcf.analyses.purity.DomainSpecificRater
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.ImpureByLackOfInformation
import org.opalj.fpcf.properties.ContextuallyPure
import org.opalj.fpcf.properties.ContextuallySideEffectFree
import org.opalj.fpcf.properties.DContextuallyPure
import org.opalj.fpcf.properties.DContextuallySideEffectFree
import org.opalj.fpcf.properties.DExternallyPure
import org.opalj.fpcf.properties.DExternallySideEffectFree
import org.opalj.fpcf.properties.DPure
import org.opalj.fpcf.properties.DSideEffectFree
import org.opalj.fpcf.properties.ExternallyPure
import org.opalj.fpcf.properties.ExternallySideEffectFree
import org.opalj.fpcf.properties.ImpureByAnalysis
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.tac.DefaultTACAIKey
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Executes a purity analysis (L2 by default) along with necessary supporting analysis.
 *
 * @author Dominik Helm
 */
object Purity {

    def usage: String = {
        "Usage: java …PurityAnalysisEvaluation \n"+
            "-cp <JAR file/Folder containing class files> OR -JDK\n"+
            "[-analysis <L0|L1|L2> (Default: L2)]\n"+
            "[-domain <class name of the domain>]\n"+
            "[-rater <class name of the rater>]\n"+
            "[-noJDK] (do not analyze any JDK methods)\n"+
            "[-individual] (reports the purity result for each method)\n"+
            "[-closedWorld] (uses closed world assumption, i.e. no class can be extended)\n"+
            "[-eagerTAC] (eagerly precompute TAC for all methods)\n"+
            "[-debug] (enable debug output from PropertyStore)\n"+
            "[-multi] (analyzes multiple projects in the subdirectories of -cp)\n"+
            "[-eval <path to evaluation directory>]\n"+
            "Example:\n\tjava …PurityAnalysisEvaluation -JDK -individual -closedWorld"
    }

    val supportingAnalyses = List(
        List(
            LazyL0FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        ),
        List(
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        ),
        List(
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyVirtualMethodStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis
        )
    )

    def evaluate(
        cp:                    File,
        analysis:              FPCFLazyAnalysisScheduler,
        domain:                SomeProject ⇒ Method ⇒ Domain with RecordDefUse,
        rater:                 DomainSpecificRater,
        withoutJDK:            Boolean,
        individual:            Boolean,
        closedWorldAssumption: Boolean,
        eagerTAC:              Boolean,
        debug:                 Boolean,
        evaluationDir:         Option[File]
    ): Unit = {
        val classFiles = JavaClassFileReader().ClassFiles(cp)
        val JDKFiles = if (withoutJDK) Traversable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        val dirName = if (cp eq JRELibraryFolder) "JDK" else cp.getName
        val projectDir = evaluationDir.map(new File(_, dirName))
        if (projectDir.isDefined && !projectDir.get.exists()) projectDir.get.mkdir()

        var projectTime: Seconds = Seconds.None
        var tacTime: Seconds = Seconds.None
        var propertyStoreTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None

        val baseConfig = ConfigFactory.load()
        implicit val config: Config = if (closedWorldAssumption) baseConfig.withValue(
            "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
        )
        else baseConfig

        val project = time {
            Project(classFiles, JDKFiles, false, Traversable.empty)
        } { t ⇒ projectTime = t.toSeconds }

        if (eagerTAC) {
            time {
                project.getOrCreateProjectInformationKeyInitializationData(SimpleAIKey, domain(project))
                val tacai = project.get(DefaultTACAIKey)
                project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
            } { t ⇒ tacTime = t.toSeconds }
        }

        val propertyStore = time {
            project.get(PropertyStoreKey)
        } { t ⇒ propertyStoreTime = t.toSeconds }

        PropertyStore.updateDebug(debug)

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
        val projMethods = for (cf ← project.allProjectClassFiles; m ← cf.methodsWithBody)
            yield declaredMethods(m._1)

        time {
            val pks: Set[PropertyKind] = support.flatMap(
                _.derives.map(_.asInstanceOf[PropertyMetaInformation].key)
            ).toSet +
                VirtualMethodPurity.key.asInstanceOf[PropertyKind] +
                fpcf.properties.Purity.key.asInstanceOf[PropertyKind]
            propertyStore.setupPhase(pks)

            for (supportAnalysis ← support) {
                supportAnalysis.startLazily(project, propertyStore)
            }

            LazyVirtualMethodPurityAnalysis.startLazily(project, propertyStore)
            analysis.startLazily(project, propertyStore)

            projMethods.foreach { dm ⇒
                propertyStore.force(dm, fpcf.properties.Purity.key)
            }
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒ analysisTime = t.toSeconds }

        if (projectDir.isDefined) {
            val runtime = new File(projectDir.get, "runtime.csv")
            val runtimeNew = !runtime.exists()
            val runtimeWriter = new PrintWriter(new FileOutputStream(runtime, true))
            try {
                if (runtimeNew) {
                    runtime.createNewFile()
                    runtimeWriter.println("project;tac;propertyStore;analysis")
                }
                runtimeWriter.println(s"$projectTime;$tacTime;$propertyStoreTime;$analysisTime;")
            } finally {
                if (runtimeWriter != null) runtimeWriter.close()
            }
        }

        val purityEs = propertyStore(projMethods, fpcf.properties.Purity.key).filter {
            case FinalEP(_, p) ⇒ p ne ImpureByLackOfInformation
            case ep            ⇒ throw new RuntimeException(s"non final purity result $ep")
        }

        val compileTimePure = purityEs.collect { case FinalEP(m: DefinedMethod, CompileTimePure) ⇒ m }
        val pure = purityEs.collect { case FinalEP(m: DefinedMethod, Pure) ⇒ m }
        val sideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, SideEffectFree) ⇒ m }
        val externallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, ExternallyPure) ⇒ m }
        val externallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, ExternallySideEffectFree) ⇒ m }
        val contextuallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallyPure) ⇒ m }
        val contextuallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, ContextuallySideEffectFree) ⇒ m }
        val dPure = purityEs.collect { case FinalEP(m: DefinedMethod, DPure) ⇒ m }
        val dSideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DSideEffectFree) ⇒ m }
        val dExternallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, DExternallyPure) ⇒ m }
        val dExternallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DExternallySideEffectFree) ⇒ m }
        val dContextuallyPure = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallyPure) ⇒ m }
        val dContextuallySideEffectFree = purityEs.collect { case FinalEP(m: DefinedMethod, DContextuallySideEffectFree) ⇒ m }
        val lbImpure = purityEs.collect { case FinalEP(m: DefinedMethod, ImpureByAnalysis) ⇒ m }

        if (projectDir.isDefined) {
            val results = new File(projectDir.get, "method-results.csv")
            val resultsNew = !results.exists()
            val resultsWriter = new PrintWriter(new FileOutputStream(results, !individual))
            try {
                if (resultsNew) {
                    results.createNewFile()
                    if (!individual)
                        resultsWriter.println("{c};{};{d},{n};{n,d},{r};{r,d},{n,r};{n,r,d},"+
                            "{p};{p,d},{n,p},{n,p,d},{i^};count")
                }

                if (!individual) {
                    resultsWriter.println(
                        s"${compileTimePure.size};${pure.size};${dPure.size};${sideEffectFree.size};"+
                            s"${dSideEffectFree.size};${externallyPure.size};${dExternallyPure.size};"+
                            s"${externallySideEffectFree.size};${dExternallySideEffectFree.size};"+
                            s"${contextuallyPure.size};${dContextuallyPure.size};"+
                            s"${contextuallySideEffectFree.size};${dContextuallySideEffectFree.size};"+
                            s"${lbImpure.size};${purityEs.size}"
                    )
                } else {
                    for (m ← compileTimePure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {c}")
                    }
                    for (m ← pure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {}")
                    }
                    for (m ← dPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {d}")
                    }
                    for (m ← sideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n}")
                    }
                    for (m ← dSideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n,d}")
                    }
                    for (m ← externallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {r}")
                    }
                    for (m ← dExternallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {r,d}")
                    }
                    for (m ← externallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n,r}")
                    }
                    for (m ← dExternallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n,r,d}")
                    }
                    for (m ← contextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {p}")
                    }
                    for (m ← dContextuallyPure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {p,d}")
                    }
                    for (m ← contextuallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n,p}")
                    }
                    for (m ← dContextuallySideEffectFree) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {n,p,d}")
                    }
                    for (m ← lbImpure) {
                        resultsWriter.println(s"${m.definedMethod.toJava} => {i}")
                    }
                }
            } finally {
                if (resultsWriter != null) resultsWriter.close()
            }
        } else {
            val result =
                propertyStore.toString(false)+
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
                    "\nImpure:                                "+lbImpure.size

            println(result)
        }
    }

    def main(args: Array[String]): Unit = {

        // Parameters:
        var cp: File = null
        var analysisName: Option[String] = None
        var domainName: Option[String] = None
        var raterName: Option[String] = None
        var withoutJDK = false
        var individual = false
        var cwa = false
        var eagerTAC = false
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
                Console.println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        while (i < args.length) {
            args(i) match {
                case "-cp"          ⇒ cp = new File(readNextArg())
                case "-analysis"    ⇒ analysisName = Some(readNextArg())
                case "-domain"      ⇒ domainName = Some(readNextArg())
                case "-rater"       ⇒ raterName = Some(readNextArg())
                case "-individual"  ⇒ individual = true
                case "-closedWorld" ⇒ cwa = true
                case "-eagerTAC"    ⇒ eagerTAC = true
                case "-debug"    ⇒ debug = true
                case "-multi"       ⇒ multiProjects = true
                case "-eval"        ⇒ evaluationDir = Some(new File(readNextArg()))
                case "-noJDK"       ⇒ withoutJDK = true
                case "-JDK"         ⇒ { cp = JRELibraryFolder; withoutJDK = true }

                case unknown ⇒
                    Console.println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }

        if (cp eq null) {
            Console.println(usage)
            throw new IllegalArgumentException(s"no classpath given (use -cp <classpath> or -JDK)")
        }

        val analysis = analysisName match {
            case Some("L0")        ⇒ LazyL0PurityAnalysis
            case Some("L1")        ⇒ LazyL1PurityAnalysis
            case None | Some("L2") ⇒ LazyL2PurityAnalysis

            case Some(a) ⇒
                Console.println(usage)
                throw new IllegalArgumentException(s"unknown analysis: $a")

        }

        val d = (p: SomeProject) ⇒ (m: Method) ⇒
            if (domainName.isEmpty) {
                new domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse(p, m)
            } else {
                // ... "org.opalj.ai.domain.l0.BaseDomainWithDefUse"
                Class.
                    forName(raterName.get).asInstanceOf[Class[Domain with RecordDefUse]].
                    getConstructor(classOf[Project[_]], classOf[Method]).
                    newInstance(p, m)
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
        println(begin.getTime)

        time {
            if (multiProjects) {
                for (subp ← cp.listFiles().filter(_.isDirectory)) {
                    println(subp.getName)
                    evaluate(
                        subp,
                        analysis,
                        d,
                        rater,
                        withoutJDK,
                        individual,
                        cwa,
                        eagerTAC,
                        debug,
                        evaluationDir
                    )
                }
            } else {
                evaluate(
                    cp,
                    analysis,
                    d,
                    rater,
                    withoutJDK,
                    individual,
                    cwa,
                    eagerTAC,
                    debug,
                    evaluationDir
                )
            }
        }(t ⇒ Console.out.println("evaluation time: "+t.toSeconds))

        val end = Calendar.getInstance()
        println(end.getTime)
    }
}
