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
package org.opalj.fpcf

import java.io.File
import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bytecode.RTJar
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.EagerVirtualMethodThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.sbt.perf.spec.PerfSpec
import org.opalj.support.info.Purity.supportingAnalyses


class L1T_ColumbusPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = locateTestResources("classfiles/Columbus 2008_10_16 - target 1.6.jar", "bi")
}

class L1T_GroovyPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = locateTestResources("classfiles/groovy-2.2.1-indy.jar", "bi")
}

class L1T_ScalaLibPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-library-2.12.4.jar", "bi")
}

class L1T_ScalaCompilerPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-compiler-2.12.4.jar", "bi")
}

class L1T_FastUtilPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = locateTestResources("classfiles/OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies/fastutil-8.1.0.jar", "bi")
}

class L1T_RtJarPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = RTJar
}

class L1T_JreLibPropertyStorePerf extends PropertyStoreScalingPerf with L1ThrownExceptionsPerf {
    override def fixutureFile: File = JRELibraryFolder
}

class L2P_ColumbusPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = locateTestResources("classfiles/Columbus 2008_10_16 - target 1.6.jar", "bi")
}

class L2P_GroovyPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = locateTestResources("classfiles/groovy-2.2.1-indy.jar", "bi")
}

class L2P_ScalaLibPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-library-2.12.4.jar", "bi")
}

class L2P_ScalaCompilerPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-compiler-2.12.4.jar", "bi")
}

class L2P_FastUtilPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = locateTestResources("classfiles/OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies/fastutil-8.1.0.jar", "bi")
}

class L2P_RtJarPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = RTJar
}

class L2P_JreLibPropertyStorePerf extends PropertyStoreScalingPerf with L2PurityPerf {
    override def fixutureFile: File = JRELibraryFolder
}

trait L1ThrownExceptionsPerf extends PropertyStoreScalingPerf {
    override def analysisName: String = "L1ThrownExceptionsAnalysis"
    override def runAnalysis(ps: PropertyStore, p: SomeProject): Unit = {
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(p, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(p, ps)
    }
}

trait L2PurityPerf extends PropertyStoreScalingPerf {
    override def analysisName: String = "L2PurityAnalysis"
    override def runAnalysis(ps: PropertyStore, p: SomeProject): Unit = {
        ps.setupPhase(
            Set(
                org.opalj.fpcf.properties.Purity.key,
                FieldMutability.key,
                ClassImmutability.key,
                TypeImmutability.key,
                VirtualMethodPurity.key,
                FieldLocality.key,
                ReturnValueFreshness.key,
                VirtualMethodReturnValueFreshness.key
            )
        )

        supportingAnalyses(2).foreach(_.startLazily(p, ps))
        EagerL2PurityAnalysis.start(p, ps)
    }
}

/**
  * Memory measurements using L1ThrownExceptionsAnalysis
  *
  * @author Andreas Muttscheller
  */
trait PropertyStoreScalingPerf extends PerfSpec {
    implicit val logContext: LogContext = GlobalLogContext

    def fixutureFile: File
    def runAnalysis(ps: PropertyStore, p: SomeProject): Unit
    def analysisName: String

    val baseConfig: Config = ConfigFactory.load()
    val propertyStoreImplementation = ConfigKeyPrefix+"PropertyStoreImplementation"

    val totalEntities = buildProject("").allClassFiles.size +
        buildProject("").allMethodsWithBody.size +
        buildProject("").allFields.size

    println(s"*** Running measurement for ${fixutureFile.getName} ***\n" +
        s"\t${buildProject("").allClassFiles.size} class files\n" +
        s"\t${buildProject("").allMethodsWithBody.size} methods with body\n" +
        s"\t${buildProject("").allFields.size} fields\n" +
        s"\t$totalEntities total entities"
    )
    Console.out.flush()

    measure(s"ReactiveAsyncPropertyStore - $analysisName - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.par.ReactiveAsyncPropertyStore")
        val ps = project.get(PropertyStoreKey)
        runAnalysis(ps, project)

        ps.waitOnPhaseCompletion()
        Runtime.getRuntime.gc()
        ps
    }

    measure(s"PKESequentialPropertyStore - $analysisName - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.seq.PKESequentialPropertyStore")
        val ps = project.get(PropertyStoreKey)
        runAnalysis(ps, project)

        ps.waitOnPhaseCompletion()
        Runtime.getRuntime.gc()
        ps
    }

    measure(s"EPKSequentialPropertyStore - $analysisName - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.seq.EPKSequentialPropertyStore")
        val ps = project.get(PropertyStoreKey)
        runAnalysis(ps, project)

        ps.waitOnPhaseCompletion()
        Runtime.getRuntime.gc()
        ps
    }

    private def buildProject(propertyStore: String): Project[URL] = {
        val testConfig = baseConfig.
            withValue(propertyStoreImplementation, ConfigValueFactory.fromAnyRef(propertyStore))

        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val fixtureClassFiles = ClassFiles(fixutureFile)

        Project(
            fixtureClassFiles,
            List.empty,
            libraryClassFilesAreInterfacesOnly = false,
            Traversable.empty,
            Project.defaultHandlerForInconsistentProjects,
            testConfig,
            logContext
        )
    }
}