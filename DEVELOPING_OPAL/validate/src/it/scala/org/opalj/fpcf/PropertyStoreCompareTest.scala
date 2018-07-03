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
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.bytecode.RTJar
import org.opalj.fpcf.PropertyStoreCompareTester.baseConfig
import org.opalj.fpcf.PropertyStoreCompareTester.fixtureProject
import org.opalj.fpcf.PropertyStoreCompareTester.propertyStoreImplementationKey
import org.opalj.fpcf.PropertyStoreCompareTester.propertyStoreList
import org.opalj.fpcf.PropertyStoreKey.ConfigKeyPrefix
import org.opalj.fpcf.analyses._
import org.opalj.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties._
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.StandardLogContext
import org.opalj.log.{Error ⇒ ErrorLogLevel}
import org.opalj.support.info.Purity.supportingAnalyses
import org.opalj.tac.DefaultTACAIKey
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

        EagerL1ThrownExceptionsAnalysis.start(fixtureProject, ps)
        EagerVirtualMethodThrownExceptionsAnalysis.start(fixtureProject, ps)

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

class PsTypeImmutabilityAnalysisTest extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(TypeImmutability.key, ClassImmutability.key)
        )
        EagerTypeImmutabilityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    it should behave like testEntitiesAndProperties[ObjectType](
        psResults,
        TypeImmutability.key,
        fixtureProject.allClassFiles.filter(_.thisType ne ObjectType.Object).map(_.thisType).toSeq
    )
}

class PsClassImmutabilityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(TypeImmutability.key, ClassImmutability.key, FieldMutability.key)
        )

        EagerClassImmutabilityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    it should behave like testEntitiesAndProperties[ObjectType](
        psResults,
        ClassImmutability.key,
        fixtureProject.allClassFiles.map(_.thisType).toSeq
    )
}

class PsL0FieldMutabilityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(FieldMutability.key)
        )
        EagerL0FieldMutabilityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    val classFileCandidates =
        if (fixtureProject.libraryClassFilesAreInterfacesOnly)
            fixtureProject.allProjectClassFiles
        else
            fixtureProject.allClassFiles

    val fields = {
        classFileCandidates.filter(cf ⇒ cf.methods.forall(m ⇒ !m.isNative)).flatMap(_.fields)
    }

    it should behave like testEntitiesAndProperties[Field](
        psResults,
        FieldMutability.key,
        fields.toSeq
    )
}

class PsL1FieldMutabilityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(FieldMutability.key)
        )
        EagerL1FieldMutabilityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    it should behave like testEntitiesAndProperties[Field](
        psResults,
        FieldMutability.key,
        fixtureProject.allFields.toSeq
    )
}

/**
 * @note needs at least 4GB RAM!
 */
class PsL0PurityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(org.opalj.fpcf.properties.Purity.key, FieldMutability.key, TypeImmutability.key)
        )
        supportingAnalyses(0).foreach(_.startLazily(fixtureProject, ps))
        EagerL0PurityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    val dms = fixtureProject.get(DeclaredMethodsKey).declaredMethods
    val methodsWithBody = dms.collect {
        case dm if dm.hasDefinition && dm.methodDefinition.body.isDefined ⇒ dm.asDefinedMethod
    }

    it should behave like testEntitiesAndProperties[DefinedMethod](
        psResults,
        org.opalj.fpcf.properties.Purity.key,
        methodsWithBody.toSeq
    )
}

/**
 * @note needs at least 8GB RAM!
 */
class PsL1PurityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        ps.setupPhase(
            Set(
                org.opalj.fpcf.properties.Purity.key,
                VirtualMethodPurity.key,
                FieldMutability.key,
                ClassImmutability.key,
                TypeImmutability.key
            )
        )
        supportingAnalyses(1).foreach(_.startLazily(fixtureProject, ps))
        EagerL1PurityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    val dms = fixtureProject.get(DeclaredMethodsKey).declaredMethods
    val methodsWithBody = dms.collect {
        case dm if dm.hasDefinition && dm.methodDefinition.body.isDefined ⇒ dm.asDefinedMethod
    }
    val configuredPurity: ConfiguredPurity = fixtureProject.get(ConfiguredPurityKey)

    it should behave like testEntitiesAndProperties[DefinedMethod](
        psResults,
        org.opalj.fpcf.properties.Purity.key,
        methodsWithBody.filterNot(configuredPurity.wasSet).toSeq
    )
}

/**
 * @note needs at least 10GB RAM!
 */
class PsL2PurityAnalysis extends PropertyStoreCompareTester {
    val psResults = executeAnalysisForEachPropertyStore() { (fixtureProject, ps) ⇒
        // Eager TAC
        val tac = fixtureProject.get(DefaultTACAIKey)
        fixtureProject.parForeachMethodWithBody() { m ⇒ tac(m.method) }

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
        supportingAnalyses(2).foreach(_.startLazily(fixtureProject, ps))
        EagerL2PurityAnalysis.start(fixtureProject, ps)

        ps.waitOnPhaseCompletion()
    }

    val dms = fixtureProject.get(DeclaredMethodsKey).declaredMethods
    val methodsWithBody = dms.collect {
        case dm if dm.hasDefinition && dm.methodDefinition.body.isDefined ⇒ dm.asDefinedMethod
    }
    val configuredPurity: ConfiguredPurity = fixtureProject.get(ConfiguredPurityKey)

    it should behave like testEntitiesAndProperties[DefinedMethod](
        psResults,
        org.opalj.fpcf.properties.Purity.key,
        methodsWithBody.filterNot(configuredPurity.wasSet).toSeq
    )
}

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
        it(s"should have computed results for scheduled entities - ${pk.toString}") {
            psResults.foreach { ps ⇒
                val el0 = ps._2.entities(pk).map(_.e).toSet

                withClue(s"${ps._1}: ") { el0.size should be >= scheduledEntities.size }

                withClue(s"${ps._1} should be contain all scheduled entities: ") {
                    assert(scheduledEntities.toSet.subsetOf(el0))
                    val diff = scheduledEntities.diff(el0.toSeq)
                    diff foreach { d ⇒
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
                            withClue(s"$e: $p - $p1: ") {
                                // We cannot compare the IntermediateEP / FinalEP / EPS directly,
                                // it compares the entity on an object identity level. In this test
                                // the objects are equal, but not on an object level.
                                p1.e should equal(p.e)
                                p1.ub should equal(p.ub)
                                p1.lb should equal(p.lb)
                            }
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