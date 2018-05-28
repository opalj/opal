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
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bytecode.RTJar
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.EagerVirtualMethodThrownExceptionsAnalysis
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.sbt.perf.spec.PerfSpec


class RtJarPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = RTJar
}

class ColumbusPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = locateTestResources("classfiles/Columbus 2008_10_16 - target 1.6.jar", "bi")
}

class GroovyPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = locateTestResources("classfiles/groovy-2.2.1-indy.jar", "bi")
}

class ScalaLibPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-library-2.12.4.jar", "bi")
}

class ScalaCompilerPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = locateTestResources("classfiles/scala-2.12.4/scala-compiler-2.12.4.jar", "bi")
}

class FastUtilPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = locateTestResources("classfiles/OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies/fastutil-8.1.0.jar", "bi")
}

class JreLibPropertyStorePerf extends L1ThrownExceptionsAnalysisPropertyStorePerf {
    override def fixutureFile: File = JRELibraryFolder
}

/**
  * Memory measurements using L1ThrownExceptionsAnalysis
  *
  * @author Andreas Muttscheller
  */
trait L1ThrownExceptionsAnalysisPropertyStorePerf extends PerfSpec {
    implicit val logContext: LogContext = GlobalLogContext

    def fixutureFile: File

    val baseConfig: Config = ConfigFactory.load()
    val propertyStoreImplementation = ConfigKeyPrefix+"PropertyStoreImplementation"

    println(s"*** Running measurement for ${fixutureFile.getName} " +
        s"containing ${buildProject("").allMethodsWithBody.size} methods with body ***")
    Console.out.flush()

    measure(s"ReactiveAsyncPropertyStore - L1ThrownExceptionsAnalysis - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.par.ReactiveAsyncPropertyStore")
        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(project, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(project, ps)

        ps.waitOnPhaseCompletion()
        Runtime.getRuntime.gc()
        ps
    }

    measure(s"PKESequentialPropertyStore - L1ThrownExceptionsAnalysis - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.seq.PKESequentialPropertyStore")
        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(project, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(project, ps)

        ps.waitOnPhaseCompletion()
        Runtime.getRuntime.gc()
        ps
    }

    measure(s"EPKSequentialPropertyStore - L1ThrownExceptionsAnalysis - ${fixutureFile.getName}") {
        val project = buildProject("org.opalj.fpcf.seq.EPKSequentialPropertyStore")
        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(project, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(project, ps)

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