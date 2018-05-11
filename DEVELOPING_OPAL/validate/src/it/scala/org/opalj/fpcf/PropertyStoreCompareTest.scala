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
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.bytecode.RTJar
import org.opalj.fpcf.PropertyStoreCompareTester.baseConfig
import org.opalj.fpcf.PropertyStoreCompareTester.fixtureProject
import org.opalj.fpcf.PropertyStoreCompareTester.propertyStoreImplementationKey
import org.opalj.fpcf.PropertyStoreCompareTester.propertyStoreList
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses._
import org.opalj.fpcf.properties._
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.StandardLogContext
import org.opalj.log.{Error ⇒ ErrorLogLevel}
import org.opalj.util.ScalaMajorVersion
import org.scalatest._

/**
 * Tests if the different PropertyStore implementations produce the same result for given
 * analysis.
 *
 * Note: Needs at least 10GB memory! (sbt -mem 10000)
 *
 * Note: Each analysis is in its own class. This is to force ScalaTest to free the memory from
 * previous runs. Putting all analysis in one test suite with multiple describe statements wouldn't
 * free the memory from previous analysis.
 *
 * Run with OPAL-Validate/it:testOnly org.opalj.fpcf.Ps*
 *
 * @author Andreas Muttscheller
 */

class PsThrownExceptionsAnalysisTest extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(ThrownExceptions.key, ThrownExceptionsByOverridingMethods.key)
        )

        L1ThrownExceptionsAnalysis.start(fixtureProject, ps)
        ThrownExceptionsByOverridingMethodsAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    it should behave like testEntitiesAndProperties[Method](
        psResults,
        ThrownExceptionsByOverridingMethods.key,
        fixtureProject.allMethodsWithBody
    )
    it should behave like testEntitiesAndProperties[Method](
        psResults,
        ThrownExceptions.key,
        fixtureProject.allMethodsWithBody
    )
}

//class PsTypeImmutabilityAnalysisTest extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
//        TypeImmutabilityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    it should behave like testEntitiesAndProperties(psResults, TypeImmutability.key)
//}
//
//class PsClassImmutabilityAnalysis extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
//        ClassImmutabilityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    it should behave like testEntitiesAndProperties(psResults, ClassImmutability.key)
//}
//
//class PsL0FieldMutabilityAnalysis extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
//        L0FieldMutabilityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    it should behave like testEntitiesAndProperties(psResults, FieldMutability.key)
//}
//
//class PsL1FieldMutabilityAnalysis extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
//        L1FieldMutabilityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    it should behave like testEntitiesAndProperties(psResults, FieldMutability.key)
//}
//
//class PsL0PurityAnalysis extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore(true) { (fixtureProject, ps) ⇒
//        L1FieldMutabilityAnalysis.start(fixtureProject, ps)
//        L0PurityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    // L0PurityAnalysis depends on TypeImmutability fallback values and L1FieldMutabilityAnalysis
//    it should behave like testEntitiesAndProperties(psResults, TypeImmutability.key)
//    it should behave like testEntitiesAndProperties(psResults, FieldMutability.key)
//    it should behave like testEntitiesAndProperties(psResults, Purity.key)
//}
//
//class PsL1PurityAnalysis extends PropertyStoreCompareTester {
//    val psResults = executeAnalysisForEachPropertyStore(true) { (fixtureProject, ps) ⇒
//        ClassImmutabilityAnalysis.start(fixtureProject, ps)
//        TypeImmutabilityAnalysis.start(fixtureProject, ps)
//        L1FieldMutabilityAnalysis.start(fixtureProject, ps)
//        L1PurityAnalysis.start(fixtureProject, ps)
//
//        ps.waitOnPhaseCompletion()
//    }
//
//    // L1PurityAnalysis depends on TypeImmutability, ClassImmutability and L1FieldMutabilityAnalysis
//    //it should behave like testEntitiesAndProperties(psResults, TypeImmutability.key)
//    //it should behave like testEntitiesAndProperties(psResults, ClassImmutability.key)
//    //it should behave like testEntitiesAndProperties(psResults, FieldMutability.key)
//    it should behave like testEntitiesAndProperties(psResults, Purity.key)
//}

trait PropertyStoreCompareTester extends FunSpec with Matchers with Inspectors with PrivateMethodTester {
    def executeAnalysisForEachPropertyStore()(
        f: (SomeProject, PropertyStore) ⇒ Unit
    ): List[(String, PropertyStore)] = {
        System.gc()
        propertyStoreList.map { psName ⇒
            val testConfig = baseConfig.
                withValue(propertyStoreImplementationKey, ConfigValueFactory.fromAnyRef(psName))
            fixtureProject = Project.recreate(fixtureProject, testConfig)

            val ps = fixtureProject.get(PropertyStoreKey)

            f(fixtureProject, ps)

            psName -> ps
        }
    }

    def testEntitiesAndProperties[E <: Entity](
        psResults:         List[(String, PropertyStore)],
        pk:                PropertyKey[Property],
        scheduledEntities: Seq[E]
    ) = {
        it(s"should have computed for scheduled entities - ${pk.toString}") {
            psResults.foreach { ps ⇒
                val el0 = ps._2.entities(pk).map(_.e).toSet

                withClue(s"${ps._1}: ") { el0.size should be >= scheduledEntities.size }

                withClue(s"${ps._1} should be contain all scheduled entities: ") {
                    assert(scheduledEntities.toSet.subsetOf(el0))
                    val diff = scheduledEntities.diff(el0.toSeq)
                    diff foreach { d =>
                        assert(!scheduledEntities.contains(d))
                    }
                }
            }
        }

        it(s"should have the same properties for ${pk.toString}") {
            scheduledEntities.foreach { e ⇒
                psResults.grouped(2).foreach { psList ⇒
                    val pl0 = psList(0)._2.properties(e).filter(_.ub.id == pk.id).toList
                    val pl1 = psList(1)._2.properties(e).filter(_.ub.id == pk.id).toList

                    withClue(s"${psList(0)._1} for entity ${e}: ") { pl0.size should be > 0 }
                    withClue(s"${psList(1)._1} for entity ${e}: ") { pl1.size should be > 0 }

                    pl0.foreach { p ⇒
                        forExactly(1, pl1) { p1 ⇒
                            withClue(s"${e} - ${psList(0)._1} - ${psList(1)._1}: ") { p1 should equal(p) }
                        }
                    }

                    assert(pl0.size == pl1.size)
                }
            }
        }
    }
}

object PropertyStoreCompareTester {

    val propertyStoreList: List[String] = List(
        "org.opalj.fpcf.par.ReactiveAsyncPropertyStore",
        "org.opalj.fpcf.seq.PKESequentialPropertyStore"
    )

    val baseConfig: Config = ConfigFactory.load()
    val propertyStoreImplementationKey: String = ConfigKeyPrefix+"PropertyStoreImplementation"
    var fixtureProject: SomeProject = buildFixtureProject(baseConfig)

    final def buildFixtureProject(testConfig: Config): Project[URL] = {
        implicit val logContext: LogContext = new StandardLogContext
        OPALLogger.register(logContext, new ConsoleOPALLogger(true, ErrorLogLevel))

        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val sourceFolder = s"DEVELOPING_OPAL/validate/target/scala-$ScalaMajorVersion/test-classes"
        val fixtureFiles = new File(sourceFolder)
        val fixtureClassFiles = ClassFiles(fixtureFiles)

        val projectClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/fixture")
        }

        val propertiesClassFiles = fixtureClassFiles.filter { cfSrc ⇒
            val (cf, _) = cfSrc
            cf.thisType.packageName.startsWith("org/opalj/fpcf/properties")
        }

        val libraryClassFiles = ClassFiles(RTJar) ++ propertiesClassFiles

        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false,
            Traversable.empty,
            Project.defaultHandlerForInconsistentProjects,
            testConfig,
            logContext
        )
    }
}