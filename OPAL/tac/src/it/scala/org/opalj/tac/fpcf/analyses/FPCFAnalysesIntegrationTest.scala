/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.reflect.runtime.universe.runtimeMirror

import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

import scala.io.Source

import com.typesafe.config.ConfigValueFactory
import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.junit.JUnitRunner

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyIsNotComputedByAnyAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePropertyKey
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysesRegistry
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.Context
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.fpcf.analyses.FPCFAnalysesIntegrationTest.factory
import org.opalj.tac.fpcf.analyses.FPCFAnalysesIntegrationTest.p
import org.opalj.tac.fpcf.analyses.FPCFAnalysesIntegrationTest.ps

/**
 * Simple test to ensure that the FPFC analyses do not cause exceptions and that their results
 * remain stable.
 *
 * @author Dominik Helm
 */
@RunWith(classOf[JUnitRunner])
class FPCFAnalysesIntegrationTest extends AnyFunSpec {

    private[this] val analysisConfigurations = getConfig

    allBIProjects(
        jreReader = None,
        config = BaseConfig.withValue(
            "org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryEntryPointsFinder")
        )
    ) foreach { biProject =>
            val (projectName, projectFactory) = biProject

            for ((name, analyses, properties) <- analysisConfigurations) {
                describe(s"the analysis configuration $name for project $projectName") {

                    it("should execute without exceptions") {
                        if (factory ne projectFactory) {
                            // Store the current factory (to distinguish the projects) and the current
                            // project so they are available for more configurations on the same project
                            factory = projectFactory
                            p = projectFactory()

                            p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                                case None =>
                                    Set(classOf[l1.DefaultDomainWithCFGAndDefUse[_]])
                                case Some(requirements) =>
                                    requirements + classOf[l1.DefaultDomainWithCFGAndDefUse[_]]
                            }
                        } else {
                            // Recreate project keeping all ProjectInformationKeys other than the
                            // PropertyStore as we are interested only in FPCF analysis results.
                            p = p.recreate { id =>
                                id != PropertyStoreKey.uniqueId &&
                                    id != FPCFAnalysesManagerKey.uniqueId &&
                                    id != CHACallGraphKey.uniqueId
                            }
                        }

                        PropertyStore.updateDebug(true)
                        ps = p.get(PropertyStoreKey)

                        time {
                            // todo do not want to run this for every setting
                            p.get(CHACallGraphKey)
                        } { t => info(s"call graph and tac analysis took ${t.toSeconds}") }

                        time { p.get(FPCFAnalysesManagerKey).runAll(analyses) }(reportAnalysisTime)
                    }

                    it("should compute the correct properties") {

                        // Get EPs for the properties we're interested in
                        // Filter for fallback property, as the entities with fallbacks may be different
                        // on each execution.
                        val actual = properties.iterator.flatMap { property =>
                            ps.entities(property.key).filter { ep =>
                                if (ep.isRefinable)
                                    fail(s"intermediate results left over $ep")
                                isRecordedProperty(property.key, ep)
                            }.map(ep => s"${ep.e} => ${ep.ub}").toSeq.sorted
                        }.toSeq

                        val actualIt = actual.iterator

                        val fileName = s"$name-$projectName.txt.gz"

                        val expectedStream = this.getClass.getResourceAsStream(fileName)
                        if (expectedStream eq null)
                            fail(
                                s"missing expected results: $name; "+
                                    s"current results written to:\n"+writeActual(actual, fileName)
                            )
                        val expectedIt =
                            Source.fromInputStream(new GZIPInputStream(expectedStream)).getLines()

                        while (actualIt.hasNext && expectedIt.hasNext) {
                            val actualLine = actualIt.next()
                            val expectedLine = expectedIt.next()
                            if (actualLine != expectedLine)
                                fail(
                                    s"comparison failed:\nnew: $actualLine\n\t\t"+
                                        s"vs.\nold: $expectedLine\n"+
                                        "current results written to :\n"+writeActual(actual, fileName)
                                )
                        }
                        if (actualIt.hasNext)
                            fail(
                                "actual is longer than expected - first line: "+actualIt.next()+
                                    "\n current results written to :\n"+writeActual(actual, fileName)
                            )
                        if (expectedIt.hasNext)
                            fail(
                                "expected is longer than actual - first line: "+expectedIt.next()+
                                    "\n current results written to :\n"+writeActual(actual, fileName)
                            )
                    }
                }
            }
        }

    def writeActual(actual: Seq[String], fileName: String): Path = {
        val path = Paths.get(fileName)
        io.writeGZip(actual.iterator.map(_ + '\n').map(_.getBytes("UTF-8")), path)
        path
    }

    def isRecordedProperty(pk: SomePropertyKey, ep: SomeEPS): Boolean = {
        // fallback properties may be set for different entities on different executions
        // because they are set lazily even for eager analyses
        ep.ub != PropertyKey.fallbackProperty(ps, PropertyIsNotComputedByAnyAnalysis, ep.e, pk) &&
            // Not analyzing the JDK, there are VirtualDeclaredMethods with Purity data
            // preconfigured that we don't want to record as they contain no additional information
            (ep.pk != Purity.key || ep.e.asInstanceOf[Context].method.hasSingleDefinedMethod)
    }

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    def getAnalysis(id: String): ComputationSpecification[FPCFAnalysis] = {
        FPCFAnalysesRegistry.eagerFactory(id.trim)
    }

    def getProperty(fqn: String): PropertyMetaInformation = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        val module = mirror.staticModule(fqn.trim)
        mirror.reflectModule(module).instance.asInstanceOf[PropertyMetaInformation]
    }

    def getConfig: Seq[(String, Set[ComputationSpecification[FPCFAnalysis]], Seq[PropertyMetaInformation])] = {
        val configInputStream =
            this.getClass.getResourceAsStream("FPCFAnalysesIntegrationTest.config")
        val configLines = Source.fromInputStream(configInputStream).getLines()

        var curConfig: (String, Set[ComputationSpecification[FPCFAnalysis]], Seq[PropertyMetaInformation]) = null
        var readProperties = false

        var configurations: Seq[(String, Set[ComputationSpecification[FPCFAnalysis]], Seq[PropertyMetaInformation])] =
            List.empty

        for (line <- configLines) {
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
}

/**
 * Stores the current project to avoid recreation when more than one configuration is run on the
 * same project.
 */
object FPCFAnalysesIntegrationTest {
    var factory: () => SomeProject = () => null
    var p: SomeProject = _
    var ps: PropertyStore = _
}
