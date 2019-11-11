/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.URL

import scala.collection.JavaConverters._
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.LogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.Field
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.DefinedMethod
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.log.DevNullLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.AllocationSiteBasedPointsToScalaCallGraphKey
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.pointsto.ArrayEntity
import org.opalj.tac.fpcf.analyses.pointsto.CallExceptions
import org.opalj.tac.fpcf.analyses.pointsto.MethodExceptions
import org.opalj.tac.fpcf.analyses.pointsto.TamiFlexKey

/**
 * Computes a call graph and reports its size.
 *
 * You can specify the call-graph algorithm:
 *  -algorithm=CHA for an CHA-based call graph
 *  -algorithm=RTA for an RTA-based call graph
 *  -algorithm=PointsTo for a points-to based call graph
 * The default algorithm is RTA.
 *
 * Please also specify whether the target (-cp=) is an application or a library using "-projectConf=".
 * Predefined configurations `ApplicationProject.conf` or `LibraryProject.conf` can be used here.
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * @author Florian Kuebler
 */
object CallGraph extends ProjectAnalysisApplication {

    OPALLogger.register(GlobalLogContext, DevNullLogger)

    override def title: String = "Call Graph Analysis"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-algorithm=CHA|RTA|PointsTo]"+
            "[-domain=domain]"+
            "[-callers=method]"+
            "[-callees=method]"+
            "[-writeCG=file]"+
            "[-writeStatistics=file]"+
            "[-writeTimings=file]"+
            "[-writePointsToSets=file]"+
            "[-main=package.MainClass]"+
            "[-tamiflex-log=logfile]"+
            "[-finalizerAnalysis=<yes|no|default>]"+
            "[-loadedClassesAnalysis=<yes|no|default>]"+
            "[-staticInitializerAnalysis=<yes|no|default>]"+
            "[-reflectionAnalysis=<yes|no|default>]"+
            "[-serializationAnalysis=<yes|no|default>]"+
            "[-threadRelatedCallsAnalysis=<yes|no|default>]"+
            "[-configuredNativeMethodsAnalysis=<yes|no|default>]"
    }

    private val algorithmRegex = "-algorithm=(CHA|RTA|PointsTo|PointsToScala)".r

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
        val remainingParameters =
            parameters.filter { p ⇒
                !p.matches(algorithmRegex.regex) &&
                    !p.startsWith("-domain=") &&
                    !p.startsWith("-callers=") &&
                    !p.startsWith("-callees=") &&
                    !p.startsWith("-writeCG=") &&
                    !p.startsWith("-writeStatistics=") &&
                    !p.startsWith("-writeTimings=") &&
                    !p.startsWith("-writePointsToSets=") && // TODO: implement this
                    !p.startsWith("-main=") &&
                    !p.startsWith("-tamiflex-log=") &&
                    !p.startsWith("-finalizerAnalysis=") &&
                    !p.startsWith("-loadedClassesAnalysis=") &&
                    !p.startsWith("-staticInitializerAnalysis=") &&
                    !p.startsWith("-reflectionAnalysis=") &&
                    !p.startsWith("-serializationAnalysis=") &&
                    !p.startsWith("-threadRelatedCallsAnalysis=") &&
                    !p.startsWith("-configuredNativeMethodsAnalysis=")
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    private[this] def performAnalysis(
        project:      Project[URL],
        calleesSigs:  List[String],
        callersSigs:  List[String],
        cgAlgorithm:  String,
        cgFile:       Option[String],
        statsFile:    Option[String],
        timingsFile:  Option[String],
        pointsToFile: Option[String],
        projectTime:  Seconds
    ): BasicReport = {
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                implicit val lg: LogContext = project.logContext
                 val ps = org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                // val ps = org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                ps
            }
        )

        // TODO: Implement output files
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm ⇒
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.toTraversable

        var propertyStoreTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        implicit val ps: PropertyStore = time { project.get(PropertyStoreKey) } { t ⇒
            propertyStoreTime = t.toSeconds
        }

        val cg = time {
            cgAlgorithm match {
                case "CHA"               ⇒ project.get(CHACallGraphKey)
                case "RTA"               ⇒ project.get(RTACallGraphKey)
                case "TypeBasedPointsTo" ⇒ project.get(TypeBasedPointsToCallGraphKey)
                case "PointsTo"          ⇒ project.get(AllocationSiteBasedPointsToCallGraphKey)
                case "PointsToScala"     ⇒ project.get(AllocationSiteBasedPointsToScalaCallGraphKey)
            }
        } { t ⇒ callGraphTime = t.toSeconds }

        try {
            ps.shutdown()
        } catch {
            case t: Throwable ⇒
                Console.err.println("PropertyStore shutdown failed: ")
                t.printStackTrace()
        }

        if (timingsFile.isDefined) {
            val runtime = new File(timingsFile.get)
            val runtimeNew = !runtime.exists()
            val runtimeWriter = new PrintWriter(new FileOutputStream(runtime, true))
            try {
                if (runtimeNew) {
                    runtime.createNewFile()
                    runtimeWriter.println("project;propertyStore;callGraph")
                }
                runtimeWriter.println(s"$projectTime;$propertyStoreTime;$callGraphTime")
            } finally {
                if (runtimeWriter != null) runtimeWriter.close()
            }
        }

        if (cgAlgorithm == "PointsTo") {
            val ptss = ps.entities(AllocationSitePointsToSet.key).toList
            import scala.collection.JavaConverters._
            val statistic = ptss.groupBy(p ⇒ p.ub.elements.size).mapValues { spts ⇒
                (spts.size, {
                    val unique = new java.util.IdentityHashMap[AllocationSitePointsToSet, AllocationSitePointsToSet]()
                    unique.putAll(spts.map(x ⇒ x.ub → x.ub).toMap.asJava)
                    unique.size()
                })
            }.map { case (size, (count, uniqueCount)) ⇒ (size, count, uniqueCount) }.toArray.sorted
            println("size, count, unique count")
            println(statistic.mkString("\n"))

            println(s"PTSs ${ptss.size}")
            println(s"PTS entries ${ptss.map(p ⇒ p.ub.elements.size).sum}")

            val byType = ptss.groupBy(_.e.getClass)

            def getNum(tpe: Class[_ <: Entity]): Int = {
              byType.get(tpe).map(_.size).getOrElse(0)
            }

            def getEntries(tpe: Class[_ <: Entity]): Int = {
              byType.get(tpe).map(_.map(_.ub.numElements).sum).getOrElse(0)
            }

            println(s"DefSite PTSs: ${getNum(classOf[DefinitionSite])}")
            println(s"Parameter PTSs: ${getNum(classOf[VirtualFormalParameter])}")
            println(s"Instance Field PTSs: ${getNum(classOf[Tuple2[Long, Field]])}")
            println(s"Static Field PTSs: ${getNum(classOf[Field])}")
            println(s"Array PTSs: ${getNum(classOf[ArrayEntity[Long]])}")
            println(s"Return PTSs: ${getNum(classOf[DefinedMethod]) + getNum(classOf[VirtualDeclaredMethod])}")
            println(s"MethodException PTSs: ${getNum(classOf[MethodExceptions])}")
            println(s"CallException PTSs: ${getNum(classOf[CallExceptions])}")

            println(s"DefSite PTS entries: ${getEntries(classOf[DefinitionSite])}")
            println(s"Parameter PTS entries: ${getEntries(classOf[VirtualFormalParameter])}")
            println(s"Instance Field PTS entries: ${getEntries(classOf[Tuple2[Long, Field]])}")
            println(s"Static Field PTS entries: ${getEntries(classOf[Field])}")
            println(s"Array PTS entries: ${getEntries(classOf[ArrayEntity[Long]])}")
            println(s"Return PTS entries: ${getEntries(classOf[DefinedMethod]) + getEntries(classOf[VirtualDeclaredMethod])}")
            println(s"MethodException PTS entries: ${getEntries(classOf[MethodExceptions])}")
            println(s"CallException PTS entries: ${getEntries(classOf[CallExceptions])}")

            /*
            //Prints sizes of all array PTSs
            for(pts <- byType(classOf[ArrayEntity[Long]]))
                println(s"${org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(pts.e.asInstanceOf[ArrayEntity[Long]].element)}\t${pts.ub.numElements}")*/

            /*
            //Prints all allocation sites in the PTS of the entity underTest in Doop's format
            val underTest = project.get(DeclaredMethodsKey)(ObjectType("java/io/UnixFileSystem"), "", ObjectType("java/io/UnixFileSystem"), "list", MethodDescriptor.withNoArgs(ArrayType(ObjectType.String)))
            val underTestPTS = ps(underTest, AllocationSitePointsToSet.key).ub
            underTestPTS.elements.foreach { as ⇒
                try {
                    val (dm, pc, tId) = org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(as)
                    println(s"<${dm.declaringClassType.toJava}: ${dm.descriptor.toJava(dm.name)}>/new ${ReferenceType.lookup(tId).toJava}")
                    if(tId < 0) {
                        val arrPTS = ps(ArrayEntity(as), AllocationSitePointsToSet.key).ub
                        arrPTS.elements.foreach {as ⇒
                            try {
                                val (dm, pc, tId) = org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(as)
                                println(s"\t<${dm.declaringClassType.toJava}: ${dm.descriptor.toJava(dm.name)}>/new ${ReferenceType.lookup(tId).toJava}")
                            } catch {
                                case _: Exception ⇒
                                    val tId = (as >> 39).toInt
                                    println(s"new ${ReferenceType.lookup(tId).toJava}")
                            }
                        }
                    }
                } catch {
                    case _: Exception ⇒
                        val tId = (as >> 39).toInt
                        println(s"new ${ReferenceType.lookup(tId).toJava}")
                }
            }*/

            /*val p2 = project.recreate(e ⇒ e != PropertyStoreKey.uniqueId && e != AllocationSiteBasedPointsToCallGraphKey.uniqueId && e != FPCFAnalysesManagerKey.uniqueId && e != AllocationSiteBasedPointsToCallGraphKey.uniqueId)
            p2.get({AllocationSiteBasedPointsToCallGraphKey})
            val ps2 = p2.get(PropertyStoreKey)

            for {
                FinalEP(e, p) ← ptss2
                ub2 = ps2(e, AllocationSitePointsToSet.key).ub
                if p.elements.size != ub2.elements.size
            } {
                println(s"$e\n\t${ub2.elements.iterator.map(org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(_)).mkString(",")}\n\t${p.elements.iterator.map(org.opalj.br.fpcf.properties.pointsto.longToAllocationSite(_)).mkString(",")}")
            }*/
        }

        val reachableMethods = cg.reachableMethods().toTraversable

        val numEdges = cg.numEdges

        println(ps.statistics.mkString("\n"))

        println(calleesSigs.mkString("\n"))
        println(callersSigs.mkString("\n"))

        for (m ← allMethods) {
            val mSig = m.descriptor.toJava(m.name)

            for (methodSignature ← calleesSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callees of ${m.toJava}:")
                    println(ps(m, Callees.key).ub.callSites().map {
                        case (pc, callees) ⇒ pc → callees.map(_.toJava).mkString(", ")
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
            for (methodSignature ← callersSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callers of ${m.toJava}:")
                    println(ps(m, Callers.key).ub.callers.map {
                        case (caller, pc, isDirect) ⇒
                            s"${caller.toJava}, $pc${if (!isDirect) ", indirect" else ""}"
                    }.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        if (cgFile.nonEmpty) {
            CallGraphSerializer.writeCG(cg, new File(cgFile.get))
        }

        if (statsFile.nonEmpty) {
            val stats = new File(statsFile.get)
            val statsWriter = new PrintWriter(new FileOutputStream(stats, true))
            try {
                stats.createNewFile()
                statsWriter.println("methods;reachable;edges")
                statsWriter.println(s"${allMethods.size};${reachableMethods.size};$numEdges")
            } finally {
                if (statsWriter != null) statsWriter.close()
            }
        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }

    // todo: we would like to print the edges for a given method
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        var tacDomain: Option[String] = None
        var calleesSigs: List[String] = Nil
        var callersSigs: List[String] = Nil
        var cgAlgorithm: String = "RTA"
        var cgFile: Option[String] = None
        var statsFile: Option[String] = None
        var timingsFile: Option[String] = None
        var pointsToFile: Option[String] = None
        var mainClass: Option[String] = None
        var tamiflexLog: Option[String] = None

        val domainRegex = "-domain=(.*)".r
        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        val writeCGRegex = "-writeCG=(.*)".r
        val writeStatsRegex = "-writeStatistics=(.*)".r
        val writeTimingsRegex = "-writeTimings=(.*)".r
        val writePointsToSetsRegex = "-writePointsToSets=(.*)".r
        val mainClassRegex = "-main=(.*)".r
        val tamiflexLogRegex = "-tamiflex-log=(.*)".r

        val finalizerAnalysisRegex = "-finalizerAnalysis=(.*)".r
        val loadedClassesAnalysisRegex = "-loadedClassesAnalysis=(.*)".r
        val staticInitializerAnalysisRegex = "-staticInitializerAnalysis=(.*)".r
        val reflectionAnalysisRegex = "-reflectionAnalysis=(.*)".r
        val serializationAnalysisRegex = "-serializationAnalysis=(.*)".r
        val threadRelatedCallsAnalysisRegex = "-threadRelatedCallsAnalysis=(.*)".r
        val configuredNativeMethodsAnalysisRegex = "-configuredNativeMethodsAnalysis=(.*)".r

        var newConfig = project.config
        var modules = newConfig.getStringList("org.opalj.tac.cg.CallGraphKey.modules").asScala.toSet

        def analyisOption(option: String, analysis: String): Unit = {
            option match {
                case "yes" ⇒
                    modules += s"org.opalj.tac.fpcf.analyses.cg.${analysis}Scheduler"
                case "no" ⇒
                    modules -= s"org.opalj.tac.fpcf.analyses.cg.${analysis}Scheduler"
                case "default" ⇒
                case _         ⇒ throw new IllegalArgumentException(s"illegal value for $analysis")
            }
        }

        parameters.foreach {
            case domainRegex(domainClass) ⇒
                if (tacDomain.isEmpty)
                    tacDomain = Some(domainClass)
                else throw new IllegalArgumentException("-domain was set twice")
            case callersRegex(methodSig) ⇒ callersSigs ::= methodSig
            case calleesRegex(methodSig) ⇒ calleesSigs ::= methodSig
            case algorithmRegex(algo)    ⇒ cgAlgorithm = algo
            case writeCGRegex(fileName) ⇒
                if (cgFile.isEmpty)
                    cgFile = Some(fileName)
                else throw new IllegalArgumentException("-writeCG was set twice")
            case writeStatsRegex(fileName) ⇒
                if (statsFile.isEmpty)
                    statsFile = Some(fileName)
                else throw new IllegalArgumentException("-writeStatistics was set twice")
            case writeTimingsRegex(fileName) ⇒
                if (timingsFile.isEmpty)
                    timingsFile = Some(fileName)
                else throw new IllegalArgumentException("-writeTimings was set twice")
            case writePointsToSetsRegex(fileName) ⇒
                if (pointsToFile.isEmpty)
                    pointsToFile = Some(fileName)
                else throw new IllegalArgumentException("-writePointsToSets was set twice")
            case mainClassRegex(fileName) ⇒
                if (mainClass.isEmpty)
                    mainClass = Some(fileName)
                else throw new IllegalArgumentException("-main was set twice")
            case tamiflexLogRegex(fileName) ⇒
                if (tamiflexLog.isEmpty)
                    tamiflexLog = Some(fileName)
                else throw new IllegalArgumentException("-tamiflex-log was set twice")
            case finalizerAnalysisRegex(option) ⇒
                analyisOption(option, "FinalizerAnalysis")
            case loadedClassesAnalysisRegex(option) ⇒
                analyisOption(option, "LoadedClassesAnalysis")
            case staticInitializerAnalysisRegex(option) ⇒
                analyisOption(option, "StaticInitializerAnalysis")
            case reflectionAnalysisRegex(option) ⇒
                analyisOption(option, "reflection.ReflectionRelatedCallsAnalysis")
            case serializationAnalysisRegex(option) ⇒
                analyisOption(option, "SerializationRelatedCallsAnalysis")
            case threadRelatedCallsAnalysisRegex(option) ⇒
                analyisOption(option, "ThreadRelatedCallsAnalysis")
            case configuredNativeMethodsAnalysisRegex(option) ⇒
                analyisOption(option, "ConfiguredNativeMethodsCallGraphAnalysis")
        }

        if (tamiflexLog.isDefined) {
            newConfig = newConfig.withValue(
                TamiFlexKey.configKey,
                ConfigValueFactory.fromAnyRef(tamiflexLog.get)
            )
            modules += "org.opalj.tac.fpcf.analyses.cg.reflection.TamiFlexCallGraphAnalysisScheduler"
        }

        if (mainClass.isDefined) {
            val key = s"${InitialEntryPointsKey.ConfigKeyPrefix}entryPoints"
            val currentValues = newConfig.getList(key).unwrapped()
            val configValue = Map(
                "declaringClass" → mainClass.get.replace('.', '/'),
                "name" → "main"
            ).asJava
            currentValues.add(ConfigValueFactory.fromMap(configValue))
            newConfig = newConfig.withValue(key, ConfigValueFactory.fromIterable(currentValues))

            newConfig = newConfig.withValue(
                s"${InitialEntryPointsKey.ConfigKeyPrefix}analysis",
                ConfigValueFactory.fromAnyRef(
                    "org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder"
                )
            )
        }

        newConfig = newConfig.withValue(
            "org.opalj.tac.cg.CallGraphKey.modules",
            ConfigValueFactory.fromIterable(modules.asJava)
        )

        var projectTime: Seconds = Seconds.None

        val newProject = time {
            Project.recreate(project, newConfig)
        } { t ⇒ projectTime = t.toSeconds }

        val domainFQN = tacDomain.getOrElse("org.opalj.ai.domain.l0.PrimitiveTACAIDomain")
        val domain = Class.forName(domainFQN).asInstanceOf[Class[Domain with RecordDefUse]]
        newProject.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               ⇒ Set(domain)
            case Some(requirements) ⇒ requirements + domain
        }

        performAnalysis(
            newProject,
            calleesSigs,
            callersSigs,
            cgAlgorithm,
            cgFile,
            statsFile,
            timingsFile,
            pointsToFile,
            projectTime
        )
    }
}
