/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.URL
import java.util.Calendar

import scala.jdk.CollectionConverters._

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
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.Field
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.DefinedMethod
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.fpcf.seq.PKESequentialPropertyStore
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CallGraphSerializer
import org.opalj.tac.cg.CFA_1_0_CallGraphKey
import org.opalj.tac.cg.CFA_1_1_CallGraphKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.CTACallGraphKey
import org.opalj.tac.cg.FTACallGraphKey
import org.opalj.tac.cg.MTACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.cg.XTACallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
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
 * Please also specify whether the target (-cp=) is an application or a library using
 * "-projectConfig=". Predefined configurations `ApplicationProject.conf` or `LibraryProject.conf`
 * can be used here.
 *
 * Furthermore, it can be used to print the callees or callers of specific methods.
 * To do so, add -callers=m, where m is the method name/signature using Java notation, as parameter
 * (for callees use -callees=m).
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
object CallGraph extends ProjectAnalysisApplication {

    //OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)

    override def title: String = "Call Graph Analysis"

    override def description: String = {
        "Provides the number of reachable methods and call edges in the give project."
    }

    override def analysisSpecificParametersDescription: String = {
        "[-algorithm=CHA|RTA|MTA|FTA|CTA|XTA|TypeBasedPointsTo|PointsTo|1-0-CFA|1-1-CFA]"+
            "[-domain=domain]"+
            "[-callers=method]"+
            "[-callees=method]"+
            "[-writeCG=file]"+
            "[-analysisName=name]"+
            "[-schedulingStrategy=name]"+
            "[-writeOutput=file]"+
            "[-j=<number of threads>]"+
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

    private val algorithmRegex =
        "-algorithm=(CHA|RTA|MTA|FTA|CTA|XTA|TypeBasedPointsTo|PointsTo|1-0-CFA|1-1-CFA)".r

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        val remainingParameters =
            parameters.filter { p =>
                !p.matches(algorithmRegex.regex) &&
                    !p.startsWith("-domain=") &&
                    !p.startsWith("-callers=") &&
                    !p.startsWith("-callees=") &&
                    !p.startsWith("-analysisName=") &&
                    !p.startsWith("-schedulingStrategy=") &&
                    !p.startsWith("-writeCG=") &&
                    !p.startsWith("-writeOutput=") &&
                    !p.startsWith("-main=") &&
                    !p.startsWith("-j=") &&
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
        analysisName: Option[String],
        cgAlgorithm:  String,
        cgFile:       Option[String],
        outputFile:   Option[String],
        numThreads:   Option[Int],
        projectTime:  Seconds
    ): BasicReport = {
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                implicit val lg: LogContext = project.logContext
                val threads =
                    numThreads.getOrElse(org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks)
                if (threads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = threads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val allMethods = declaredMethods.declaredMethods.filter { dm =>
            dm.hasSingleDefinedMethod &&
                (dm.definedMethod.classFile.thisType eq dm.declaringClassType)
        }.to(Iterable)

        var propertyStoreTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        implicit val ps: PropertyStore = time { project.get(PropertyStoreKey) } { t =>
            propertyStoreTime = t.toSeconds
        }

        val cg = time {
            cgAlgorithm match {
                case "CHA"               => project.get(CHACallGraphKey)
                case "RTA"               => project.get(RTACallGraphKey)
                case "MTA"               => project.get(MTACallGraphKey)
                case "FTA"               => project.get(FTACallGraphKey)
                case "CTA"               => project.get(CTACallGraphKey)
                case "XTA"               => project.get(XTACallGraphKey)
                case "TypeBasedPointsTo" => project.get(TypeBasedPointsToCallGraphKey)
                case "PointsTo"          => project.get(AllocationSiteBasedPointsToCallGraphKey)
                case "1-0-CFA"           => project.get(CFA_1_0_CallGraphKey)
                case "1-1-CFA"           => project.get(CFA_1_1_CallGraphKey)
            }
        } { t => callGraphTime = t.toSeconds }

        try {
            ps.shutdown()
        } catch {
            case t: Throwable =>
                Console.err.println("PropertyStore shutdown failed: ")
                t.printStackTrace()
        }

        if (cgAlgorithm == "PointsTo") {
            val ptss = ps.entities(AllocationSitePointsToSet.key).toList

            println(s"PTSs ${ptss.size}")
            println(s"PTS entries ${ptss.map(p => p.ub.elements.size).sum}")

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
        }

        val reachableContexts = cg.reachableMethods().to(Iterable)
        val reachableMethods = reachableContexts.map(_.method).toSet

        val numEdges = cg.numEdges

        println(ps.statistics.mkString("\n"))

        println(calleesSigs.mkString("\n"))
        println(callersSigs.mkString("\n"))

        implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

        for (m <- allMethods) {
            val mSig = m.descriptor.toJava(m.name)

            for (methodSignature <- calleesSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callees of ${m.toJava}:")
                    val calleesProperty = ps(m, Callees.key).ub
                    println(calleesProperty.callerContexts.flatMap { context =>
                        calleesProperty.callSites(context).map {
                            case (pc, callees) => pc -> callees.map(_.method.toJava).mkString(", ")
                        }.toSet.mkString("\t", "\n\t", "\n")
                    })
                }
            }
            for (methodSignature <- callersSigs) {
                if (mSig.contains(methodSignature)) {
                    println(s"Callers of ${m.toJava}:")
                    println(ps(m, Callers.key).ub.callers(m).iterator.map {
                        case (caller, pc, isDirect) =>
                            s"${caller.toJava}, $pc${if (!isDirect) ", indirect" else ""}"
                    }.iterator.mkString("\t", "\n\t", "\n"))
                }
            }
        }

        if (cgFile.nonEmpty) {
            CallGraphSerializer.writeCG(cg, new File(cgFile.get))
        }

        if (outputFile.isDefined) {
            val output = new File(outputFile.get)
            val newOutputFile = !output.exists()
            val outputWriter = new PrintWriter(new FileOutputStream(output, true))
            try {
                if (newOutputFile) {
                    output.createNewFile()
                    outputWriter.println(
                        "analysisName;project time;propertyStore time;callGraph time;total time;"+
                            "methods;reachable;edges"
                    )
                }

                val totalTime = projectTime + propertyStoreTime + callGraphTime
                outputWriter.println(
                    s"${analysisName.get};${projectTime.toString(false)};"+
                        s"${propertyStoreTime.toString(false)};"+
                        s"${callGraphTime.toString(false)};${totalTime.toString(false)};"+
                        s"${allMethods.size};${reachableMethods.size};$numEdges"
                )

            } finally {
                outputWriter.close()
            }

        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable contexts: ${reachableContexts.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }

    // todo: we would like to print the edges for a given method
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        var tacDomain: Option[String] = None
        var calleesSigs: List[String] = Nil
        var callersSigs: List[String] = Nil
        var cgAlgorithm: String = "RTA"
        var analysisName: Option[String] = None
        var schedulingStrategy: Option[String] = None
        var cgFile: Option[String] = None
        var outputFile: Option[String] = None
        var mainClass: Option[String] = None
        var tamiflexLog: Option[String] = None
        var numThreads: Option[Int] = None

        val domainRegex = "-domain=(.*)".r
        val callersRegex = "-callers=(.*)".r
        val calleesRegex = "-callees=(.*)".r
        val analysisNameRegex = "-analysisName=(.*)".r
        val schedulingStrategyRegex = "-schedulingStrategy=(.*)".r
        val writeCGRegex = "-writeCG=(.*)".r
        val writeOutputRegex = "-writeOutput=(.*)".r
        val numThreadsRegex = "-j=(.*)".r
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
                case "yes" =>
                    modules += s"org.opalj.tac.fpcf.analyses.cg.${analysis}Scheduler"
                case "no" =>
                    modules -= s"org.opalj.tac.fpcf.analyses.cg.${analysis}Scheduler"
                case "default" =>
                case _         => throw new IllegalArgumentException(s"illegal value for $analysis")
            }
        }

        parameters.foreach {
            case domainRegex(domainClass) =>
                if (tacDomain.isEmpty)
                    tacDomain = Some(domainClass)
                else throw new IllegalArgumentException("-domain was set twice")
            case callersRegex(methodSig) => callersSigs ::= methodSig
            case calleesRegex(methodSig) => calleesSigs ::= methodSig
            case algorithmRegex(algo)    => cgAlgorithm = algo
            case analysisNameRegex(name) =>
                if (analysisName.isEmpty)
                    analysisName = Some(name)
                else throw new IllegalArgumentException("-analysisName was set twice")
            case schedulingStrategyRegex(name) =>
                if (schedulingStrategy.isEmpty)
                    schedulingStrategy = Some(name)
                else throw new IllegalArgumentException("-schedulingStrategy was set twice")
            case numThreadsRegex(threads) =>
                if (numThreads.isEmpty)
                    numThreads = Some(Integer.parseInt(threads))
                else throw new IllegalArgumentException("-j was set twice")
            case writeCGRegex(fileName) =>
                if (cgFile.isEmpty)
                    cgFile = Some(fileName)
                else throw new IllegalArgumentException("-writeCG was set twice")
            case writeOutputRegex(fileName) =>
                if (outputFile.isEmpty)
                    outputFile = Some(fileName)
                else throw new IllegalArgumentException("-writeOutput was set twice")
            case mainClassRegex(fileName) =>
                if (mainClass.isEmpty)
                    mainClass = Some(fileName)
                else throw new IllegalArgumentException("-main was set twice")
            case tamiflexLogRegex(fileName) =>
                if (tamiflexLog.isEmpty)
                    tamiflexLog = Some(fileName)
                else throw new IllegalArgumentException("-tamiflex-log was set twice")
            case finalizerAnalysisRegex(option) =>
                analyisOption(option, "FinalizerAnalysis")
            case loadedClassesAnalysisRegex(option) =>
                analyisOption(option, "LoadedClassesAnalysis")
            case staticInitializerAnalysisRegex(option) =>
                analyisOption(option, "StaticInitializerAnalysis")
            case reflectionAnalysisRegex(option) =>
                analyisOption(option, "reflection.ReflectionRelatedCallsAnalysis")
            case serializationAnalysisRegex(option) =>
                analyisOption(option, "SerializationRelatedCallsAnalysis")
            case threadRelatedCallsAnalysisRegex(option) =>
                analyisOption(option, "ThreadRelatedCallsAnalysis")
            case configuredNativeMethodsAnalysisRegex(option) =>
                analyisOption(option, "ConfiguredNativeMethodsCallGraphAnalysis")
        }

        if (tamiflexLog.isDefined) {
            newConfig = newConfig.withValue(
                TamiFlexKey.configKey,
                ConfigValueFactory.fromAnyRef(tamiflexLog.get)
            )
            modules += "org.opalj.tac.fpcf.analyses.cg.reflection.TamiFlexCallGraphAnalysisScheduler"
        }

        if (schedulingStrategy.isDefined) {
            newConfig = newConfig.withValue(
                PKESequentialPropertyStore.TasksManagerKey,
                ConfigValueFactory.fromAnyRef(schedulingStrategy.get)
            )
        }

        if (mainClass.isDefined) {
            val key = s"${InitialEntryPointsKey.ConfigKeyPrefix}entryPoints"
            val currentValues = newConfig.getList(key).unwrapped()
            val configValue = Map(
                "declaringClass" -> mainClass.get.replace('.', '/'),
                "name" -> "main"
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
        } { t => projectTime = t.toSeconds }

        val domainFQN = tacDomain.getOrElse("org.opalj.ai.domain.l0.PrimitiveTACAIDomain")
        val domain = Class.forName(domainFQN).asInstanceOf[Class[Domain with RecordDefUse]]
        newProject.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        if (analysisName.isEmpty) {
            analysisName = Some(s"RUN-${Calendar.getInstance().getTime().toString}")
        }

        performAnalysis(
            newProject,
            calleesSigs,
            callersSigs,
            analysisName,
            cgAlgorithm,
            cgFile,
            outputFile,
            numThreads,
            projectTime
        )
    }
}
