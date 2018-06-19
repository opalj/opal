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
package fpcf
package analyses

import java.io.File
import java.io.PrintWriter

import scala.reflect.runtime.universe.runtimeMirror
import scala.io.Source
import scala.io.Codec.UTF8
import org.junit.runner.RunWith
import org.opalj.br.DeclaredMethod
import org.scalatest.FunSpec
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.FPCFAnalysesIntegrationTest.p
import org.opalj.fpcf.analyses.FPCFAnalysesIntegrationTest.factory
import org.opalj.fpcf.analyses.FPCFAnalysesIntegrationTest.ps
import org.opalj.util.Nanoseconds
import org.scalatest.junit.JUnitRunner
import org.opalj.fpcf.properties.Purity

/**
 * Simple test to ensure that the FPFC analyses do not cause exceptions and that their results
 * remain stable.
 *
 * @author Dominik Helm
 */
@RunWith(classOf[JUnitRunner])
class FPCFAnalysesIntegrationTest extends FunSpec {

    private[this] val analysisConfigurations = getConfig

    allBIProjects(jreReader = None) foreach { biProject ⇒
        val (projectName, projectFactory) = biProject

        for ((name, analyses, properties) ← analysisConfigurations) {
            describe(s"the analysis configuration $name for project $projectName") {

                it("should execute without exceptions") {
                    if (factory ne projectFactory) {
                        // Store the current factory (to distinguish the projects) and the current
                        // project so they are available for more configurations on the same project
                        factory = projectFactory
                        p = projectFactory()
                    } else {
                        // Recreate project keeping all ProjectInformationKeys other than the
                        // PropertyStore as we are interested only in FPCF analysis results.
                        p = p.recreate { id ⇒
                            id != PropertyStoreKey.uniqueId && id != FPCFAnalysesManagerKey.uniqueId
                        }
                    }

                    ps = p.get(PropertyStoreKey)

                    time {
                        p.get(FPCFAnalysesManagerKey).runAll(analyses)
                    }(reportAnalysisTime)
                }

                it("should compute the correct properties") {

                    // Get EPs for the properties we're interested in
                    // Filter for fallback property, as the entities with fallbacks may be different
                    // on each execution.
                    val actualProperties = properties.iterator.flatMap { property ⇒
                        ps.entities(property.key).filter { ep ⇒
                            if (ep.isRefinable)
                                fail(s"intermediate results left over ${ep}")
                            !ep.e.toString().contains("Lambda$") &&
                                ep.ub != PropertyKey.fallbackProperty(ps, ep.e, property.key) &&
                                (ep.pk != Purity.key ||
                                    ep.e.asInstanceOf[DeclaredMethod].hasDefinition)
                        }.toSeq.sortBy(_.e.toString)
                    }

                    val actual = actualProperties.map(ep ⇒ s"${ep.e} => ${ep.ub}").toSeq
                    val actualIt = actual.iterator

                    val fileName = s"$name-$projectName.txt"

                    val expectedInputStream = this.getClass.getResourceAsStream(fileName)
                    if (expectedInputStream eq null)
                        fail(
                            s"missing expected results: $name; "+
                                s"current results written to:\n${writeResults(fileName, actual)}"
                        )
                    val expectedIt = Source.fromInputStream(expectedInputStream)(UTF8).getLines

                    while (actualIt.hasNext && expectedIt.hasNext) {
                        val actualLine = actualIt.next()
                        val expectedLine = expectedIt.next()
                        if (actualLine != expectedLine)
                            fail(
                                s"comparison failed:\n$actualLine\n\t\tvs.\n$expectedLine\n"+
                                    "current results written to :\n"+writeResults(fileName, actual)
                            )
                    }
                    if (actualIt.hasNext)
                        fail(
                            "actual is longer than expected - first line: "+actualIt.next()+
                                "\n current results written to :\n"+writeResults(fileName, actual)
                        )
                    if (expectedIt.hasNext)
                        fail(
                            "expected is longer than actual - first line: "+expectedIt.next()+
                                "\n current results written to :\n"+writeResults(fileName, actual)
                        )
                }
            }
        }
    }

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    def getAnalysis(id: String): ComputationSpecification = {
        FPCFAnalysesRegistry.eagerFactory(id.trim)
    }

    def getProperty(fqn: String): PropertyMetaInformation = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        val module = mirror.staticModule(fqn.trim)
        mirror.reflectModule(module).instance.asInstanceOf[PropertyMetaInformation]
    }

    def getConfig: Seq[(String, Set[ComputationSpecification], Seq[PropertyMetaInformation])] = {
        val configInputStream =
            this.getClass.getResourceAsStream("FPCFAnalysesIntegrationTest.config")
        val configLines = Source.fromInputStream(configInputStream)(UTF8).getLines()

        var curConfig: (String, Set[ComputationSpecification], Seq[PropertyMetaInformation]) = null
        var readProperties = false

        var configurations: Seq[(String, Set[ComputationSpecification], Seq[PropertyMetaInformation])] =
            List.empty

        for (line ← configLines) {
            if (line.startsWith(" ")) {
                if (readProperties)
                    curConfig = (curConfig._1, curConfig._2, curConfig._3 :+ getProperty(line))
                else
                    curConfig = (curConfig._1, curConfig._2 + getAnalysis(line), curConfig._3)
            } else if (line.startsWith("=>")) {
                readProperties = true
            } else {
                if (!line.isEmpty) {
                    if (curConfig != null) configurations :+= curConfig
                    curConfig = (line, Set.empty, Seq.empty)
                }
                readProperties = false
            }
        }
        if (curConfig != null) configurations :+= curConfig

        configurations
    }

    def writeResults(fileName: String, actual: Seq[String]): String = {
        val file = new File(fileName)
        val out = new PrintWriter(file, "UTF-8")
        try {
            actual.foreach(out.println)
        } finally {
            out.close()
        }
        file.getAbsolutePath
    }
}

/**
 * Stores the current project to avoid recreation when more than one configuration is run on the
 * same project.
 */
object FPCFAnalysesIntegrationTest {
    var factory: () ⇒ SomeProject = () ⇒ null
    var p: SomeProject = null
    var ps: PropertyStore = null
}
