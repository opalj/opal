/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.reference.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.br.ObjectType
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.br.Field
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.fpcf.Entity
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.MutableType_new
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.reference.LazyL0ReferenceImmutabilityAnalysis
import java.nio.file.Path
import org.opalj.br.analyses.Project
import org.opalj.log.DevNullLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.Calendar
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStoreContext
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.opalj.support.info.RunningAnalyses.RunningAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import java.nio.file.FileSystems
/**
 * Determines the immutability of references, fields, classes and types of a project
 *
 * @author Tobias Peter Roth
 */
object RunningAnalyses extends Enumeration {
    type RunningAnalysis = Value
    val References, Fields, Classes, Types, All = Value
}

object Immutability {
    def evaluate(
        cp:                    File,
        analysis:              RunningAnalysis,
        numThreads:            Int,
        projectDir:            Option[String],
        libDir:                Option[String],
        resultsFolder:         Path,
        timeEvaluation:        Boolean,
        threadEvaluation:      Boolean,
        withoutJDK:            Boolean,
        isLibrary:             Boolean,
        closedWorldAssumption: Boolean
    ): BasicReport = {
        import org.opalj.ai.fpcf.properties.AIDomainFactoryKey

        OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)

        val classFiles = projectDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = libDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ Traversable.empty
        }

        val JDKFiles = if (withoutJDK) Traversable.empty
        else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        // TODO: use variables for the constants
        implicit var config: Config = if (isLibrary)
            ConfigFactory.load("LibraryProject.conf")
        else
            ConfigFactory.load("ApplicationProject.conf")

        // TODO: in case of application this value is already set
        if (closedWorldAssumption) {
            import com.typesafe.config.ConfigValueFactory
            config = config.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
        }

        var projectTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        val project = time {
            Project(
                classFiles,
                libFiles ++ JDKFiles,
                libraryClassFilesAreInterfacesOnly = false,
                Traversable.empty
            )
        } { t ⇒ projectTime = t.toSeconds }

        val referenceDependencies: List[FPCFAnalysisScheduler] = List(
            EagerL0ReferenceImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis_new,
            LazyInterProceduralEscapeAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )
        val fieldDependencies: List[FPCFAnalysisScheduler] = List(
            LazyL0ReferenceImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis_new,
            EagerL0FieldImmutabilityAnalysis,
            LazyLxClassImmutabilityAnalysis_new,
            LazyLxTypeImmutabilityAnalysis_new,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )
        val classDepencencies: List[FPCFAnalysisScheduler] = List(
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis_new,
            LazyL0ReferenceImmutabilityAnalysis,
            LazyL0FieldImmutabilityAnalysis,
            LazyLxTypeImmutabilityAnalysis_new,
            EagerLxClassImmutabilityAnalysis_new,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )
        val typeDependencies: List[FPCFAnalysisScheduler] = List(
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis_new,
            LazyL0ReferenceImmutabilityAnalysis,
            LazyL0FieldImmutabilityAnalysis,
            LazyLxClassImmutabilityAnalysis_new,
            EagerLxTypeImmutabilityAnalysis_new,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )
        val allImmAnalysisDependencies: List[FPCFAnalysisScheduler] =
            List(
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis_new,
                EagerL0ReferenceImmutabilityAnalysis,
                EagerL0FieldImmutabilityAnalysis,
                EagerLxClassImmutabilityAnalysis_new,
                EagerLxTypeImmutabilityAnalysis_new,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )
        var dependencies: List[FPCFAnalysisScheduler] = List.empty
        if (analysis == RunningAnalyses.References)
            dependencies = referenceDependencies
        if (analysis == RunningAnalyses.Fields)
            dependencies = fieldDependencies
        if (analysis == RunningAnalyses.Classes)
            dependencies = classDepencencies
        if (analysis == RunningAnalyses.Types)
            dependencies = typeDependencies
        if (analysis == RunningAnalyses.All)
            dependencies = allImmAnalysisDependencies

        L2PurityAnalysis_new.setRater(Some(SystemOutLoggingAllExceptionRater))

        var propertyStore: PropertyStore = null

        val analysesManager = project.get(FPCFAnalysesManagerKey)

        time {
            analysesManager.project.get(RTACallGraphKey)
        } { t ⇒ callGraphTime = t.toSeconds }

        analysesManager.project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            import java.net.URL

            import org.opalj.ai.domain
            //Set[Class[_ <: AnyRef]](classOf[domain.l0.BaseDomainWithDefUse[URL]])
            Set[Class[_ <: AnyRef]](classOf[domain.l1.DefaultDomainWithCFGAndDefUse[URL]])
        }
        //val propertyStore = time { project.get(PropertyStoreKey) } { t ⇒ propertyStoreTime = t.toSeconds }

        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                import org.opalj.log.LogContext
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.ParTasksManagerConfig.MaxThreads = numThreads
                    // FIXME: this property store is broken
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )

        time {
            propertyStore = analysesManager
                .runAll(
                    dependencies
                )
                ._1
            propertyStore.waitOnPhaseCompletion()

        } { t ⇒
            analysisTime = t.toSeconds
        }

        val stringBuilderResults: StringBuilder = new StringBuilder()

        val allfieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet
        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        //References
        val mutableReferences = propertyStore
            .finalEntities(MutableReference)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)

        val lazyInitializedReferencesThreadSafe = propertyStore
            .finalEntities(LazyInitializedThreadSafeReference)
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)

        val lazyInitializedReferencesNotThreadSafeButDeterministic = propertyStore
            .finalEntities(LazyInitializedNotThreadSafeButDeterministicReference)
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)

        val notThreadSafeLazyInitialization = propertyStore
            .finalEntities(LazyInitializedNotThreadSafeReference)
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)
        val immutableReferences = propertyStore
            .entities(
                eps ⇒ //allfieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]) &&
                    eps.isFinal && (eps.asFinal.p match {
                        case ImmutableReference ⇒ true
                        case _                  ⇒ false
                    })
            )
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)

        //Test....
        //mutableReferences = mutableReferences ++ notThreadSafeLazyInitialization
        // for comparison reasons
        if (analysis == RunningAnalyses.All || analysis == RunningAnalyses.References) {
            stringBuilderResults.append(s"""
| mutable References:
| ${mutableReferences.mkString("|| mutable Reference \n")}
|
| lazy initalized not thread safe and not deterministic references:
| ${notThreadSafeLazyInitialization.sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString).mkString("|| no ts & n dt ref\n")}
|
| lazy initialized not thread safe but deterministic references:
| ${lazyInitializedReferencesNotThreadSafeButDeterministic.mkString("|| li no ts b dt\n")}
|
| lazy initialized thread safe reference:
| ${lazyInitializedReferencesThreadSafe.mkString("|| li thread safe\n")}
|
| immutable Reference:
| ${immutableReferences.mkString("|| immutable References \n")}
|
|""".stripMargin)
        }

        //Fields
        val mutableFields = propertyStore
            .finalEntities(MutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList.sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)
        val shallowImmutableFields = propertyStore
            .finalEntities(ShallowImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList

        val dependentImmutableFields = propertyStore
            .finalEntities(DependentImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList

        val deepImmutableFields = propertyStore
            .finalEntities(DeepImmutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
        if (analysis == RunningAnalyses.All || analysis == RunningAnalyses.Fields) {
            stringBuilderResults.append(
                s"""
               | mutable fields:
               | ${mutableFields.mkString(" || mutable Field \n")}
               |
               | shallow immutable fields:
               | ${shallowImmutableFields.mkString(" || shallow immutable field \n")}
               |
               | dependent immutable fields:
               | ${dependentImmutableFields.mkString(" || dependent immutable field \n")}
               |
               | deep immutable fields:
               | ${deepImmutableFields.mkString(" || deep immutable field \n")}
               |
               |""".stripMargin
            )

        }

        //Classes
        val mutableClasses = propertyStore
            .finalEntities(MutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        val shallowImmutableClasses = propertyStore
            .finalEntities(ShallowImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList

        val dependentImmutableClasses = propertyStore
            .finalEntities(DependentImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val deepImmutables = propertyStore
            .finalEntities(DeepImmutableClass)
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
            .toList
        val deepImmutableClassesInterfaces = deepImmutables
            .filter(x ⇒ x.isInstanceOf[ObjectType] && allInterfaces.contains(x.asInstanceOf[ObjectType]))
            .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))
        val deepImmutableClasses =
            deepImmutables
                .filter(!deepImmutableClassesInterfaces.toSet.contains(_))
                .filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType]))

        if (analysis == RunningAnalyses.All || analysis == RunningAnalyses.Classes) {
            stringBuilderResults.append(
                s"""
               | mutable classes:
               | ${mutableClasses.mkString(" || mutable class \n")}
               |
               | shallow immutable classes:
               | ${shallowImmutableClasses.mkString(" || shallow immutable classes \n")}
               |
               | dependent immutable classes:
               | ${dependentImmutableClasses.mkString(" || dependent immutable classes \n")}
               |
               | deep immutable classes:
               | ${deepImmutableClasses.mkString(" || deep immutable classes \n")}
               |
               |""".stripMargin
            )
        }

        //Types
        val allProjectClassFilesIterator = project.allProjectClassFiles
        val types =
            allProjectClassFilesIterator.filter(_.thisType ne ObjectType.Object).map(_.thisType).toSet
        val mutableTypes = propertyStore
            .finalEntities(MutableType_new)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList

        val shallowImmutableTypes = propertyStore
            .finalEntities(ShallowImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        val dependentImmutableTypes = propertyStore
            .finalEntities(DependentImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        val deepImmutableTypes = propertyStore
            .finalEntities(DeepImmutableType)
            .filter({ x ⇒
                types.contains(x.asInstanceOf[ObjectType])
            })
            .toList
        if (analysis == RunningAnalyses.All || analysis == RunningAnalyses.Types) {
            stringBuilderResults.append(
                s"""
               | mutable types:
               | ${mutableTypes.mkString(" || mutable types \n")}
               |
               | shallow immutable types:
               | ${shallowImmutableTypes.mkString(" || shallow immutable types \n")}
               |
               | dependent immutable types:
               | ${dependentImmutableTypes.mkString(" || dependent immutable types \n")}
               |
               | deep immutable types:
               | ${deepImmutableTypes.mkString(" || deep immutable types\n")}
               |""".stripMargin
            )
        }

        val stringBuilderAmounts: StringBuilder = new StringBuilder

        if (analysis == RunningAnalyses.References || analysis == RunningAnalyses.All) {
            stringBuilderAmounts.append(
                s"""
   | mutable References: ${mutableReferences.size}
   | lazy initialized not thread safe ref.: ${notThreadSafeLazyInitialization.size}
   | lazy initialization not thread safe but deterministic: ${lazyInitializedReferencesNotThreadSafeButDeterministic.size}
   | lazy initialization thread safe: ${lazyInitializedReferencesThreadSafe.size}
   | immutable references: ${immutableReferences.size}
   | references: ${allfieldsInProjectClassFiles.size}
   |""".stripMargin
            )
        }
        if (analysis == RunningAnalyses.Fields || analysis == RunningAnalyses.All) {
            stringBuilderAmounts.append(
                s"""
   | mutable fields: ${mutableFields.size}
   | shallow immutable fields: ${shallowImmutableFields.size}
   | dependent immutable fields: ${dependentImmutableFields.size}
   | deep immutable fields: ${deepImmutableFields.size}
   | fields: ${allfieldsInProjectClassFiles.size}
   |""".stripMargin
            )
        }
        if (analysis == RunningAnalyses.Classes || analysis == RunningAnalyses.All) {
            stringBuilderAmounts.append(
                s"""
   | mutable classes: ${mutableClasses.size}
   | shallow immutable classes: ${shallowImmutableClasses.size}
   | dependent immutable classes: ${dependentImmutableClasses.size}
   | deep immutable classes: ${deepImmutableClasses.size}
   | classes: ${allProjectClassFilesIterator.size}
   |
   |""".stripMargin
            )
        }
        if (analysis == RunningAnalyses.Types || analysis == RunningAnalyses.All)
            stringBuilderAmounts.append(
                s"""
    | mutable types: ${mutableTypes.size}
    | shallow immutable types: ${shallowImmutableTypes.size}
    | dependent immutable types: ${dependentImmutableTypes.size}
    | deep immutable types: ${deepImmutableTypes.size}
    |
    |""".stripMargin
            )

        val totalTime = projectTime + callGraphTime + analysisTime // + propertyStoreTime
        //   $propertyStoreTime seconds propertyStoreTime
        stringBuilderAmounts.append(
            s"""
            | running ${analysis.toString} analysis
            | took  $totalTime total time
            |   $projectTime seconds projectTime
            |   $callGraphTime seconds callGraphTime
            |   $analysisTime seconds analysisTime
            | $analysisTime seconds
            | with $numThreads threads
            |""".stripMargin
        )
        println(
            s"""
|
| ${stringBuilderAmounts.toString()}
|
| Time Results:
|
| $numThreads Threads :: took $analysisTime
|
|""".stripMargin
        )
        println("resultsfolder: "+resultsFolder)

        val calendar = Calendar.getInstance()
        if (resultsFolder != null) {
            val file = new File(
                s"${if (resultsFolder != null) resultsFolder.toAbsolutePath.toString else "."}"+
                    s""+
                    s"/${analysis.toString}_${calendar.get(Calendar.YEAR)}_"+
                    s"${(calendar.get(Calendar.MONTH) + 1)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                    s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                    s"${calendar.get(Calendar.SECOND)}_${numThreads}Threads.txt"
            )
            file.createNewFile()
            val bw = new BufferedWriter(new FileWriter(file))
            bw.write(s""" ${stringBuilderResults.toString()}
                      |
                      | ${stringBuilderAmounts.toString()}
                      |"""".stripMargin)
            bw.close()
        }
        println(s"propertyStore: ${propertyStore.getClass.toString()}")
        println(s"jdk folder: $JRELibraryFolder")

        BasicReport(
            stringBuilderAmounts.toString()
        )
    }

    def main(args: Array[String]): Unit = {
        def usage: String = {
            s"""
               | Usage: java …ImmutabilityAnalysisEvaluation
               | -cp <JAR file/Folder containing class files> OR -JDK
               | [-analysis <imm analysis that should be executed: References, Fields, Classes, Types, All>]
               | [-threads <threads that should be max used>]
               | [-resultFolder <folder for the result files>]
               | [-closedWorld] (uses closed world assumption, i.e. no class can be extended)
               | [-noJDK] (running without the JDK)
               |""".stripMargin
        }
        var i = 0
        var cp: File = null
        var resultFolder: Path = null
        var numThreads = 0
        var timeEvaluation: Boolean = false
        var threadEvaluation: Boolean = false
        var projectDir: Option[String] = None
        var libDir: Option[String] = None
        var withoutJDK: Boolean = false
        var closedWorldAssumption = false
        var isLibrary = false

        def readNextArg(): String = {
            i = i + 1
            if (i < args.length) {
                args(i)
            } else {
                println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        var analysis: RunningAnalysis = RunningAnalyses.All

        while (i < args.length) {
            args(i) match {
                case "-analysis" ⇒ {
                    val result = readNextArg()
                    if (result == "All")
                        analysis = RunningAnalyses.All
                    else if (result == "References")
                        analysis = RunningAnalyses.References
                    else if (result == "Fields")
                        analysis = RunningAnalyses.Fields
                    else if (result == "Classes")
                        analysis = RunningAnalyses.Classes
                    else if (result == "Types")
                        analysis = RunningAnalyses.Types
                    else throw new IllegalArgumentException(s"unknown parameter: $result")
                }
                case "-threads" ⇒ numThreads = readNextArg().toInt
                case "-cp"      ⇒ cp = new File(readNextArg())
                case "-resultFolder" ⇒
                    resultFolder = FileSystems.getDefault().getPath(readNextArg())
                case "-timeEvaluation"   ⇒ timeEvaluation = true
                case "-threadEvaluation" ⇒ threadEvaluation = true
                case "-projectDir"       ⇒ projectDir = Some(readNextArg())
                case "-libDir"           ⇒ libDir = Some(readNextArg())
                case "-closedWorld"      ⇒ closedWorldAssumption = true
                case "-isLibrary"        ⇒ isLibrary = true
                case "-noJDK"            ⇒ withoutJDK = true
                case "-JDK" ⇒
                    cp = JRELibraryFolder; withoutJDK = true
                case unknown ⇒
                    Console.println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }
        evaluate(cp, analysis, numThreads, projectDir, libDir, resultFolder, timeEvaluation, threadEvaluation, withoutJDK, isLibrary, closedWorldAssumption)
    }
}

