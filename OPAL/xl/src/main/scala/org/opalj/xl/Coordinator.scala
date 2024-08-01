/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl

import java.net.URL
import java.util.concurrent.TimeUnit

import org.opalj.xl.connector.svf.AllocationSiteBasedSVFConnectorDetectorScheduler

import org.opalj.fpcf.FinalEP
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.tac.ComputeTACAIKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedLibraryPointsToAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.tac.DUVar
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.fpcf.analyses.EagerFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.xl.logger.PointsToInteractionLogger
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult

import org.opalj.fpcf.FinalP
import org.opalj.value.ValueInformation

object Coordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    type V = DUVar[ValueInformation]

    case class ScriptEngineInstance[T](element: T)

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val start = System.currentTimeMillis

        var analyses: List[FPCFAnalysisScheduler] = List(LazyTACAIProvider)

        project.updateProjectInformationKeyInitializationData(ContextProviderKey) {
            case _ => new AllocationSitesPointsToTypeIterator(project)
        }
        implicit val logContext: LogContext = project.logContext
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                // Some integrated analyses cannot cope with multiple threads
                org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = 1
                org.opalj.fpcf.par.PKECPropertyStore(context: _*)
            }

        )

        analyses ++= AllocationSiteBasedPointsToCallGraphKey.allCallGraphAnalyses(project)
        analyses ++= Iterable(
            AllocationSiteBasedLibraryPointsToAnalysisScheduler,
            AllocationSiteBasedSVFConnectorDetectorScheduler,
            LazyTypeImmutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyL2FieldAssignabilityAnalysis,
            EagerFieldImmutabilityAnalysis
        )

        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

        val tacaiKey = project.get(ComputeTACAIKey)

        val defSites = project.get(DefinitionSitesKey).definitionSites.keySet()

        //  val cg = project.get(AllocationSiteBasedPointsToCallGraphKey)

          project.allMethods.map(method=> {
            val callees = propertyStore(method, Callees.key)
            val callers = propertyStore(method, Callers.key)
            println(
                s""" method: $method
                   | method descriptor: ${method.descriptor.toJVMDescriptor}
                   | callees: $callees
                   | callers: $callers
                   |""".stripMargin)
        }
           )


         println("TAJS results:")
          project.allProjectClassFiles
            .flatMap(_.methods)
            .foreach(method => {
                propertyStore(method, AnalysisResult.key) match {
                    case FinalP(InterimAnalysisResult(tajsStore)) =>
                        println(s"TAJS result: $tajsStore")
                    case x => println(s"other case: $x")
                }
            })
        println()
         println("OPAL results:")
        defSites.forEach(defSite => {
            propertyStore(defSite, AllocationSitePointsToSet.key) match {
                case FinalEP(DefinitionSite(method, pc), pointsToSet) =>
                    println(method)
                    try {
                        val taCode = tacaiKey(method)
                        val stmts = taCode.stmts
                        if (pointsToSet.elements.size > 0)
                            println(
                                s"""
                   | method ${defSite.method} pc ${defSite.pc}
                   | DefSite: ${stmts(taCode.pcToIndex(defSite.pc)).asAssignment.targetVar}
                   | PointsToSet (${pointsToSet.numElements}): ${
                                    pointsToSet.elements.iterator
                                        .map(long => ReferenceType.lookup(pointsto.allocationSiteLongToTypeId(long)))
                                        .mkString("\n")
                                }
                   | =================================== """.stripMargin
                            )
                    } catch {
                        case _: Throwable =>
                    }
                case x => println(x)
            }
        })
        val fields = propertyStore.entities(FieldImmutability.key).toList

        val transitivelyImmutable = fields.filter(ep => ep.asEPS.ub == TransitivelyImmutableField).size
        val nonTransitivelyImmutable = fields.filter(ep => ep.asEPS.ub == NonTransitivelyImmutableField).size
        val dependentlyImmutable = fields.filter(ep => ep.asEPS.ub.isInstanceOf[DependentlyImmutableField]).size
        val mutable = fields.filter(ep => ep.asEPS.ub == MutableField).size
        println(
            s"""
               | transitivelyImmutable fields: $transitivelyImmutable
               | ${fields.filter(ep => ep.asEPS.ub == TransitivelyImmutableField).mkString("\n")}
               | dependently Immutable fields: $dependentlyImmutable
               | non transitively Immutable fields: $nonTransitivelyImmutable
               | mutable fields: $mutable
               |
               |""".stripMargin
        )

        println(
            s"""
               | ---------------------------
               | Logging Results:
               | Java to Native Calls: ${PointsToInteractionLogger.javaToNativeCalls.size}
               | Native to Java Calls: ${PointsToInteractionLogger.nativeToJavaCalls.size}
               |""".stripMargin
        )
        println(s"java to native calls points to sets sizes:")
        PointsToInteractionLogger.javaToNativeCalls.toList.foreach(x => println(x._2.size))
        println(s"native to java calls points to sets sizes:")
        PointsToInteractionLogger.nativeToJavaCalls.toList.foreach(x => println(x._2.size))
        val timeNeeded = System.currentTimeMillis - start;
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeNeeded)
        println(s"seconds needed: $seconds")
        BasicReport("")
    }
}
