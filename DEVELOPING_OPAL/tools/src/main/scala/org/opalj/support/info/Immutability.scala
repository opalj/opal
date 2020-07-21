/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.fpcf.PropertyStoreKey
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
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
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
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.log.LogContext
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

/**
 * Determines the immutability of ref, fields, classes and types of a project
 *
 * @author Tobias Peter Roth
 */
object Immutability {

    import java.io.File

    import org.opalj.support.info.Immutability.RunningAnalysis.RunningAnalysis

    //override def title: String = "Immutability Analysis"

    //override def description: String = "determines the immutability of references, fields, classes and types"

    object RunningAnalysis extends Enumeration {
        type RunningAnalysis = Value
        val References, Fields, Classes, Types, All = Value
    }
    import java.io.BufferedWriter
    import java.io.FileWriter
    import java.util.Calendar

    import RunningAnalysis._
    def evaluate(
        analysis:       RunningAnalysis,
        numThreads:     Int,
        cp:             File,
        resultsFolder:  Path,
        timeEvaluation: Boolean

    ): BasicReport = {
        import scala.collection.mutable

        OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)
        val classFiles = JavaClassFileReader().ClassFiles(cp)
        /*
     val classFiles = projectDir match {
       case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
       case None      ⇒ JavaClassFileReader().ClassFiles(cp)
     }

     val libFiles = libDir match {
       case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
       case None      ⇒ Traversable.empty
     }*/

        val JDKFiles = /*if (withoutJDK) Traversable.empty
     else */ JavaClassFileReader().ClassFiles(JRELibraryFolder)

        //println("JDKFiles: "+JDKFiles.size)

        val project = //time {
            Project(
                classFiles, // JDKFiles,
                JDKFiles, //libFiles ++
                libraryClassFilesAreInterfacesOnly = false,
                Traversable.empty
            )
        //} { t ⇒ projectTime = t.toSeconds }

        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.
        //var t = Seconds.None
        //val ps = time {

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None

        val ra: RunningAnalysis = All

        val referenceDependencies: List[FPCFAnalysisScheduler] = List(
            EagerL0ReferenceImmutabilityAnalysis,
            LazyL0FieldImmutabilityAnalysis,
            LazyLxClassImmutabilityAnalysis_new,
            LazyLxTypeImmutabilityAnalysis_new,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis_new,
            LazyInterProceduralEscapeAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyLxTypeImmutabilityAnalysis_new
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
        val allImmAnalysisDepencies: List[FPCFAnalysisScheduler] =
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
        if (ra == References)
            dependencies = referenceDependencies
        if (ra == Fields)
            dependencies = fieldDependencies
        if (ra == Classes)
            dependencies = classDepencencies
        if (ra == Types)
            dependencies = typeDependencies
        if (ra == All)
            dependencies = allImmAnalysisDepencies

        //var timeResults: List[Seconds]  = List.empty
        val threadTimeResults: mutable.HashMap[Int, List[Seconds]] = mutable.HashMap.empty
        var threads: List[Int] = List.empty
        if (timeEvaluation)
            threads = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
        else
            threads = List(0)

        for (thread ← threads) {
            var timeResults: List[Seconds] = List.empty
            for (i ← 1 to (if (timeEvaluation) 10 else 1)) {
                import java.net.URL
                val newProject: Project[URL] = project.recreate()
                val analysesManager = newProject.get(FPCFAnalysesManagerKey)
                analysesManager.project.get(RTACallGraphKey)

                analysesManager.project.recreate()
                time {
                    propertyStore = analysesManager
                        .runAll(
                            dependencies
                        )
                        ._1
                    //val numThreads = 18
                    newProject.getOrCreateProjectInformationKeyInitializationData(
                        PropertyStoreKey,
                        (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                            implicit val lg: LogContext = project.logContext
                            if (numThreads == 0) {
                                org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                            } else {
                                org.opalj.fpcf.par.ParTasksManagerConfig.MaxThreads = thread
                                // FIXME: this property store is broken
                                org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                            }
                        }
                    )
                    //new PKECPropertyStore(context, project.logContext)
                    //org.opalj.fpcf.par.ParTasksManagerConfig.MaxThreads = 8
                    //var context: Map[Class[_], AnyRef] = Map.empty

                    propertyStore.waitOnPhaseCompletion()

                } { t ⇒
                    analysisTime = t.toSeconds
                }
                timeResults = analysisTime :: timeResults
            }
            threadTimeResults.put(thread, timeResults)
        }

        val stringBuilderResults: StringBuilder = new StringBuilder()

        val allfieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet
        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        println("all fields in project class files: "+allfieldsInProjectClassFiles.size)
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

        val notThreadSafeOrNotDeterministicLazyInitialization = propertyStore
            .finalEntities(LazyInitializedNotThreadSafeOrNotDeterministicReference)
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)
        val immutableReferences = propertyStore
            .entities(
                eps ⇒ //allfieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]) &&
                    eps.isFinal && (eps.asFinal.p match {
                        case ImmutableReference(_) ⇒ true
                        case _                     ⇒ false
                    })
            )
            .toList
            .sortWith((e1: Entity, e2: Entity) ⇒ e1.toString < e2.toString)

        stringBuilderResults.append(
            s"""
             | mutable References:
             | ${mutableReferences.mkString("|| mutable Reference \n")}
             |
             | lazy initalized not thread safe and not deterministic references:
             | ${notThreadSafeOrNotDeterministicLazyInitialization.mkString("|| no ts & n dt ref\n")}
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
             |""".stripMargin
        )

        //Fields
        val mutableFields = propertyStore
            .finalEntities(MutableField)
            .filter(x ⇒ allfieldsInProjectClassFiles.contains(x.asInstanceOf[Field]))
            .toList
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
        stringBuilderResults.append(
            s"""
               | mutable fields:
               |
               |
               | shallow immutable fields:
               |
               |
               | dependent immutable fields:
               |
               |
               | deep immutable fields:
               |
               |""".stripMargin
        )

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
        val stringBuilderAmounts: StringBuilder = new StringBuilder

        if (ra == References || ra == All) {
            stringBuilderAmounts.append(
                s"""
                   | mutable References: ${mutableReferences.size}
                   | lazy initialized not thread safe or not deterministic ref.: ${notThreadSafeOrNotDeterministicLazyInitialization.size}
                   | lazy initialization not thread safe but deterministic: ${lazyInitializedReferencesNotThreadSafeButDeterministic.size}
                   | lazy initialization thread safe: ${lazyInitializedReferencesThreadSafe.size}
                   | immutable references: ${immutableReferences.size}
                   |""".stripMargin
            )
        }
        if (ra == Fields || ra == All) {
            stringBuilderAmounts.append(
                s"""
                   | mutable fields: ${mutableFields.size}
                   | shallow immutable fields: ${shallowImmutableFields.size}
                   | depenent immutable fields: ${dependentImmutableFields.size}
                   | deep immutable fields: ${deepImmutableFields.size}
                   |""".stripMargin
            )
        }
        if (ra == Classes || ra == All) {
            stringBuilderAmounts.append(
                s"""
                   | mutable classes: ${mutableClasses.size}
                   | shallow immutable classes: ${shallowImmutableClasses.size}
                   | depenent immutable classes: ${dependentImmutableClasses.size}
                   | deep immutable classes: ${deepImmutableClasses.size}
                   |""".stripMargin
            )
        }
        if (ra == Types || ra == All)
            stringBuilderAmounts.append(
                s"""
       | mutable types: ${mutableTypes.size}
       | shallow immutable types: ${shallowImmutableTypes.size}
       | dependent immutable types: ${dependentImmutableTypes.size}
       | deep immutable types: ${deepImmutableTypes.size}
       |
       |""".stripMargin
            )

        stringBuilderAmounts.append(
            s"""
               |took $analysisTime seconds
               |""".stripMargin
        )

        /*(o._1, o._2.iterator.fold(0.0,
            {(x:Double,y:Seconds)⇒x+y.timeSpan})))*/

        threadTimeResults.map(x ⇒ (x._1, x._2.fold(Seconds.None)((x, y) ⇒ x + y).timeSpan / threadTimeResults.size))

        println(
            s""" ${stringBuilderResults.toString()}
               |
               | ${stringBuilderAmounts.toString()}
               |
               | Time Results:
               | ${threadTimeResults.map(x ⇒ s"""${x._1} Thread :: as average ${x._2} seconds""")}
               |
               |"""".stripMargin
        )

        val calendar = Calendar.getInstance()
        val file = new File(
            s"${resultsFolder.toAbsolutePath.toString}/wholeImmResult_${calendar.get(Calendar.YEAR)}_"+
                s"${calendar.get(Calendar.MONTH)}_${calendar.get(Calendar.DAY_OF_MONTH)}_"+
                s"${calendar.get(Calendar.HOUR_OF_DAY)}_${calendar.get(Calendar.MINUTE)}_"+
                s"${calendar.get(Calendar.MILLISECOND)}.txt"
        )
        file.createNewFile()
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(s""" ${stringBuilderResults.toString()}
                  |
                  | ${stringBuilderAmounts.toString()}
                  |"""".stripMargin)
        bw.close()
        BasicReport(
            stringBuilderAmounts.toString()
        )
    }

    def main(args: Array[String]): Unit = {
        def usage: String = {
            "Usage: java …ImmutabilityAnalysisEvaluation \n"+
                "-cp <JAR file/Folder containing class files> OR -JDK\n"+
                "[-analysis <imm analysis that should be executed: References, Fields, Classes, Types, All>]\n"+
                "[-threads <threads that should be max used>]\n"+
                "[-resultFolder <folder for the result files>]\n" /*+
                "[-projectDir <directory with project class files relative to cp>]\n"+
                "[-libDir <directory with library class files relative to cp>]\n"+
                "[-analysis <L0|L1|L2> (Default: L2, the most precise analysis configuration)]\n"+
                "[-fieldMutability <none|L0|L1|L2> (Default: Depends on analysis level)]\n"+
                "[-escape <none|L0|L1> (Default: L1, the most precise configuration)]\n"+
                "[-domain <class name of the abstract interpretation domain>]\n"+
                "[-rater <class name of the rater for domain-specific actions>]\n"+
                "[-callGraph <CHA|RTA|PointsTo> (Default: RTA)]\n"+
                "[-eager] (supporting analyses are executed eagerly)\n"+
                "[-noJDK] (do not analyze any JDK methods)\n"+
                "[-individual] (reports the purity result for each method)\n"+
                "[-closedWorld] (uses closed world assumption, i.e. no class can be extended)\n"+
                "[-library] (assumes that the target is a library)\n"+
                "[-debug] (enable debug output from PropertyStore)\n"+
                "[-multi] (analyzes multiple projects in the subdirectories of -cp)\n"+
                "[-eval <path to evaluation directory>]\n"+
                "[-j <number of threads to be used> (0 for the sequential implementation)]\n"+
                "[-analysisName <analysisName which defines the analysis within the results file>]\n"+
                "[-schedulingStrategy <schedulingStrategy which defines the analysis within the results file>]\n"+
                "Example:\n\tjava …PurityAnalysisEvaluation -JDK -individual -closedWorld"*/
        }
        var i = 0
        var cp: File = null
        var resultFolder: Path = null
        var numThreads = 0
        var timeEvaluation: Boolean = false

        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        var analysis: RunningAnalysis = RunningAnalysis.All
        //val numberOfThreads:Option[String] = None

        while (i < args.length) {
            args(i) match {
                case "-analysis" ⇒ {
                    val result = readNextArg()
                    if (result == "All")
                        analysis = RunningAnalysis.All
                    else if (result == "References")
                        analysis = RunningAnalysis.References
                    else if (result == "Fields")
                        analysis = RunningAnalysis.Fields
                    else if (result == "Classes")
                        analysis = RunningAnalysis.Classes
                    else if (result == "Types")
                        analysis = RunningAnalysis.Fields
                }
                case "-threads" ⇒ numThreads = readNextArg().toInt
                case "-cp"      ⇒ cp = new File(readNextArg())
                case "-resultFolder" ⇒
                    import java.nio.file.FileSystems

                    resultFolder = FileSystems.getDefault().getPath(readNextArg())
                case "-timeEvaluation" ⇒ timeEvaluation = true
                /*
        case "-projectDir"         ⇒ projectDir = Some(readNextArg())
        case "-libDir"             ⇒ libDir = Some(readNextArg())
        case "-analysis"           ⇒ analysisName = Some(readNextArg())
        case "-fieldMutability"    ⇒ fieldMutabilityAnalysisName = Some(readNextArg())
        case "-escape"             ⇒ escapeAnalysisName = Some(readNextArg())
        case "-domain"             ⇒ domainName = Some(readNextArg())
        case "-rater"              ⇒ raterName = Some(readNextArg())
        case "-callGraph"          ⇒ callGraphName = Some(readNextArg())
        case "-analysisName"       ⇒ configurationName = Some(readNextArg())
        case "-schedulingStrategy" ⇒ schedulingStrategy = Some(readNextArg())
        case "-eager"              ⇒ eager = true
        case "-individual"         ⇒ individual = true
        case "-closedWorld"        ⇒ cwa = true
        case "-library"            ⇒ isLibrary = true
        case "-debug"              ⇒ debug = true
        case "-multi"              ⇒ multiProjects = true
        case "-eval"               ⇒ evaluationDir = Some(new File(readNextArg()))
        case "-j"                  ⇒ numThreads = readNextArg().toInt
        case "-noJDK"              ⇒ withoutJDK = true */
                case "-JDK" ⇒
                    cp = JRELibraryFolder; //withoutJDK = true

                case unknown ⇒
                    Console.println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }

        evaluate(analysis, numThreads, cp, resultFolder, timeEvaluation)
    }
}

