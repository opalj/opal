/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl

import java.net.URL
import java.util.concurrent.TimeUnit

import org.opalj.xl.logger.PointsToInteractionLogger

import org.opalj.br.Method
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.cg.NoCalleesDueToNotReachableMethod
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
//import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.tac.DUVar
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.EagerFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedLibraryPointsToAnalysisScheduler
import org.opalj.value.ValueInformation
import org.opalj.xl.connector.svf.AllocationSiteBasedSVFConnectorDetectorScheduler
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult

object Coordinator extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] {

    type V = DUVar[ValueInformation]

    case class ScriptEngineInstance[T](element: T)

    implicit val logContext: LogContext = GlobalLogContext

    override final val analysis = this

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
            LazyTypeImmutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyL2FieldAssignabilityAnalysis,
            EagerFieldImmutabilityAnalysis
        )

        val crossLanguage = true
        if (crossLanguage) {
            analyses ++= Iterable(AllocationSiteBasedSVFConnectorDetectorScheduler)
        }

        println(org.opalj.bytecode.JRELibraryFolder)

        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(analyses)

        // val tacaiKey = project.get(ComputeTACAIKey)

        val defSites = project.get(DefinitionSitesKey).definitionSites.keySet()

        //  val cg = project.get(AllocationSiteBasedPointsToCallGraphKey)

        /*  project.allMethods.map(method => {
            val callees = propertyStore(method, Callees.key)
            val callers = propertyStore(method, Callers.key)
            println(
                        s"""
                           |  method: ${method.name}
                           | callees: $callees
                           | callers: $callers
                           |""".stripMargin)
        }
        ) */

        println("TAJS results:")
        project.allProjectClassFiles
            .flatMap(_.methods)
            .foreach(method => {
                propertyStore(method, AnalysisResult.key) match {
                    case FinalP(InterimAnalysisResult(tajsStore)) =>
                    // println(s"TAJS result: $tajsStore")
                    case x => println(s"other case: $x")
                }
            })
        println()
        println("OPAL results:")
        defSites.forEach(defSite => {
            propertyStore(defSite, AllocationSitePointsToSet.key) match {
                case FinalEP(DefinitionSite(method, pc), pointsToSet) =>
                    // println(method)
                    try {
                        // val taCode = tacaiKey(method)
                        // val stmts = taCode.stmts
                        // if (pointsToSet.elements.size > 0)
                        /*  println(
                                s"""
                                   | method ${defSite.method} pc ${defSite.pc}

                                   | DefSite: ${stmts(taCode.pcToIndex(defSite.pc)).asAssignment.targetVar}

                                   | PointsToSet (${pointsToSet.numElements}): ${
                                    pointsToSet.elements.iterator
                                        .map(long => ReferenceType.lookup(pointsto.allocationSiteLongToTypeId(long)))
                                        .mkString("\n")
                                }
                   | =================================== """.stripMargin
                            ) */
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

        val timeNeeded = System.currentTimeMillis - start;
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeNeeded)

        var firstRM = true
        var recheableMethods = 0

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        var rms = List.empty[Method]
        for {
            cf <- project.allProjectClassFiles
            method <- cf.methods
        } {
            val dm = declaredMethods(method)
            if ((!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods) ||
                (dm.hasSingleDefinedMethod && dm.definedMethod.classFile.thisType == dm.declaringClassType)
            ) {
                val calleeEOptP = propertyStore(dm, Callees.key)
                //     val callerEOptP = propertyStore(dm, Callers.key)
                if (calleeEOptP.ub ne NoCalleesDueToNotReachableMethod) {
                    if (firstRM) {
                        firstRM = false
                    } else {
                        rms = method :: rms
                        /// println(s"method: $method ${method.isNative}")
                        /// println(s"callees $calleeEOptP")
                        recheableMethods = recheableMethods + 1
                        calleeEOptP match {
                            case FinalEP(_, NoCallees) =>
                            case FinalEP(_, callees: Callees) =>
                                implicit val contextProvider = project.get(ContextProviderKey)
                                implicit val ps: PropertyStore = propertyStore
                                for {
                                    callerContext <- callees.callerContexts
                                    (pc, targets) <- callees.callSites(callerContext)
                                    target <- targets
                                } {
                                    /// println(s" callee: $target")
                                }
                            case _ => throw new RuntimeException()
                        }

                        //  if (method.isNative)
                        //      throw new Exception("finished...")
                    }
                }
                // implicit val contextProvider = project.get(ContextProviderKey)
                // callerEOptP.ub.callers(dm).iterator.toList.foreach(x=>println(s"caller method: ${x._1.name} ${x._1.definedMethod.isNative}"))
            }
        }

        BasicReport(s"""
               | transitively immutable fields: $transitivelyImmutable
               | dependently Immutable fields: $dependentlyImmutable
               | non transitively Immutable fields: $nonTransitivelyImmutable
               | mutable fields: $mutable
               |
               | Java to Native Calls: ${PointsToInteractionLogger.javaToNativeCalls.size}
               | Java to native calls points to sets sizes: ${PointsToInteractionLogger.javaToNativeCalls.toList.map(
                x => x._2.size
            ).mkString("\n")}
               | Native to Java Calls: ${PointsToInteractionLogger.nativeToJavaCalls.size}
               | Native to java calls points to sets sizes: ${PointsToInteractionLogger.nativeToJavaCalls.toList.map(
                x => x._2.size
            ).mkString("\n")}
               |
               | Native to Java field writes: ${}
               | Native to Java field reads
               |
               | reachable Methods $recheableMethods
               |
               |""".stripMargin)
    }
}
