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
import org.opalj.br.analyses.Project
import org.opalj.bytecode.RTJar
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.fpcf.analyses.EagerVirtualMethodThrownExceptionsAnalysis
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.sbt.perf.spec.PerfSpec

/**
  * Example performance test that sleeps for 100ms and returnes a 1MB array.
  *
  * @author Andreas Muttscheller
  */
class PropertyStorePerf extends PerfSpec {
    implicit val logContext: LogContext = GlobalLogContext

    val baseConfig: Config = ConfigFactory.load()
    val propertyStoreImplementation = ConfigKeyPrefix+"PropertyStoreImplementation"

    measure("ReactiveAsyncPropertyStore - L1ThrownExceptionsAnalysis") {
        val project = buildProject("org.opalj.fpcf.par.ReactiveAsyncPropertyStore")
        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(project, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(project, ps)

        ps.waitOnPhaseCompletion()
        ps
    }

    measure("PKESequentialPropertyStore - L1ThrownExceptionsAnalysis") {
        val project = buildProject("org.opalj.fpcf.seq.PKESequentialPropertyStore")
        val ps = project.get(PropertyStoreKey)
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        EagerL1ThrownExceptionsAnalysis.start(project, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(project, ps)

        ps.waitOnPhaseCompletion()
        ps
    }

    private def buildProject(propertyStore: String): Project[URL] = {
        val testConfig = baseConfig.
            withValue(propertyStoreImplementation, ConfigValueFactory.fromAnyRef(propertyStore))

        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val fixtureFiles = new File(RTJar.getAbsolutePath)
        val fixtureClassFiles = ClassFiles(fixtureFiles)

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