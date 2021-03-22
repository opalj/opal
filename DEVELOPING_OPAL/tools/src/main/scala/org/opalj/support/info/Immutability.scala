/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.nio.file.Path
import java.io.IOException
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.Calendar
import java.nio.file.FileSystems
import java.net.URL
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

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
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.br.Field
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.ImmutableFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableFieldReference
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.fpcf.Entity
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.tac.fpcf.analyses.immutability.LazyL3FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1TypeImmutabilityAnalysis
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.log.LogContext
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.AbstractCallGraphKey
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.EPS
import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyStore

/**
 * Determines the immutability of field references, fields, classes and types and provides several setting
 * options for the evaluation.
 *
 * @author Tobias Roth
 */
object Immutability {

    sealed trait Analyses
    case object FieldReferences extends Analyses
    case object Fields extends Analyses
    case object Classes extends Analyses
    case object Types extends Analyses
    case object All extends Analyses

    def evaluate(
        cp:                                File,
        analysis:                          Analyses,
        numThreads:                        Int,
        projectDir:                        Option[String],
        libDir:                            Option[String],
        resultsFolder:                     Path,
        withoutJDK:                        Boolean,
        isLibrary:                         Boolean,
        closedWorldAssumption:             Boolean,
        callGraphKey:                      AbstractCallGraphKey,
        level:                             Int,
        withoutConsiderGenericity:         Boolean,
        withoutConsiderLazyInitialization: Boolean,
        configurationName:                 Option[String],
        times:                             Int
    ): BasicReport = {

        val classFiles = projectDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = libDir match {
            case Some(dir) ⇒ JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      ⇒ Traversable.empty
        }

        val JDKFiles =
            if (withoutJDK) Traversable.empty
            else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        // TODO: use variables for the constants
        implicit var config: Config =
            if (isLibrary)
                ConfigFactory.load("LibraryProject.conf")
            else
                ConfigFactory.load("CommandLineProject.conf")

        // TODO: in case of application this value is already set
        if (closedWorldAssumption) {
            config = config.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
        }

        config = config.withValue(
            "org.opalj.fpcf.analyses.L3FieldImmutabilityAnalysis.considerGenericity",
            ConfigValueFactory.fromAnyRef(!withoutConsiderGenericity)
        )

        config = config.withValue(
            "org.opalj.fpcf.analyses.L0FieldReferenceImmutabilityAnalysis.considerLazyInitialization",
            ConfigValueFactory.fromAnyRef(!withoutConsiderLazyInitialization)
        )

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
        } { t ⇒
            projectTime = t.toSeconds
        }

        val allProjectClassTypes = project.allProjectClassFiles.toIterator.map(_.thisType).toSet

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet

        val dependencies: List[FPCFAnalysisScheduler] =
            List(
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                LazyL0FieldReferenceImmutabilityAnalysis,
                LazyL3FieldImmutabilityAnalysis,
                LazyL1ClassImmutabilityAnalysis,
                LazyL1TypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )

        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            if (level == 0)
                Set[Class[_ <: AnyRef]](classOf[domain.l0.BaseDomainWithDefUse[URL]])
            else if (level == 1)
                Set[Class[_ <: AnyRef]](classOf[domain.l1.DefaultDomainWithCFGAndDefUse[URL]])
            else if (level == 2)
                Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
            else
                throw new Exception(s"The level $level does not exist")
        }

        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = numThreads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )
        time { project.get(callGraphKey) } { t ⇒
            callGraphTime = t.toSeconds
        }
        var propertyStore: PropertyStore = null

        val analysesManager = project.get(FPCFAnalysesManagerKey)

        time {
            propertyStore = analysesManager.runAll(dependencies)._1
            analysis match {
                case FieldReferences ⇒
                    allFieldsInProjectClassFiles.foreach(
                        f ⇒ propertyStore.force(f, br.fpcf.properties.FieldReferenceImmutability.key)
                    )
                case Fields ⇒
                    allFieldsInProjectClassFiles.foreach(
                        f ⇒ propertyStore.force(f, br.fpcf.properties.FieldImmutability.key)
                    )
                case Classes ⇒
                    allProjectClassTypes.foreach(
                        c ⇒ propertyStore.force(c, br.fpcf.properties.ClassImmutability.key)
                    )
                case Types ⇒
                    allProjectClassTypes.foreach(
                        c ⇒ propertyStore.force(c, br.fpcf.properties.TypeImmutability.key)
                    )
                case All ⇒
                    allFieldsInProjectClassFiles.foreach(f ⇒ {
                        propertyStore.force(f, br.fpcf.properties.FieldReferenceImmutability.key)
                        propertyStore.force(f, br.fpcf.properties.FieldImmutability.key)
                    })
                    allProjectClassTypes.foreach(c ⇒ {
                        propertyStore.force(c, br.fpcf.properties.ClassImmutability.key)
                        propertyStore.force(c, br.fpcf.properties.TypeImmutability.key)
                    })
            }
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒
            analysisTime = t.toSeconds
        }

        val stringBuilderResults: StringBuilder = new StringBuilder()

        val fieldReferenceGroupedResults = propertyStore
            .entities(FieldReferenceImmutability.key)
            .filter(field ⇒ allFieldsInProjectClassFiles.contains(field.e.asInstanceOf[Field]))
            .toTraversable
            .groupBy(_.asFinal.p)

        def unpackFieldEPS(eps: EPS[Entity, OrderedProperty]): String = {
            if (!eps.e.isInstanceOf[Field])
                throw new Exception(s"${eps.e} is not a field")
            val field = eps.e.asInstanceOf[Field]
            val fieldName = field.name
            val packageName = field.classFile.thisType.packageName.replace("/", ".")
            val className = field.classFile.thisType.simpleName
            packageName+"."+className+"."+fieldName
        }

        val mutableFieldReferences =
            fieldReferenceGroupedResults
                .getOrElse(MutableFieldReference, Iterator.empty)
                .map(unpackFieldEPS)
                .toSeq
                .sortWith(_ < _)

        val notThreadSafeLazyInitializedFieldReferences =
            fieldReferenceGroupedResults
                .getOrElse(LazyInitializedNotThreadSafeFieldReference, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val lazyInitializedReferencesNotThreadSafeButDeterministic =
            fieldReferenceGroupedResults
                .getOrElse(LazyInitializedNotThreadSafeButDeterministicFieldReference, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val threadSafeLazyInitializedFieldReferences =
            fieldReferenceGroupedResults
                .getOrElse(LazyInitializedThreadSafeFieldReference, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val immutableReferences = fieldReferenceGroupedResults
            .getOrElse(ImmutableFieldReference, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        if (analysis == All || analysis == FieldReferences) {
            stringBuilderResults.append(
                s"""
                | Mutable References:
                | ${mutableFieldReferences.map(_+" | Mutable Reference ").mkString("\n")}
                |
                | Lazy Initialized Not Thread Safe And Not Deterministic Field References:
                | ${
                    notThreadSafeLazyInitializedFieldReferences
                        .map(_+" | Lazy Initialized Not Thread Safe And Not Deterministic Field Reference")
                        .mkString("\n")
                }
                |
                | Lazy Initialized Not Thread Safe But Deterministic Field References:
                | ${
                    lazyInitializedReferencesNotThreadSafeButDeterministic
                        .map(_+" | Lazy Initialized Not Thread Safe But Deterministic Field Reference")
                        .mkString("\n")
                }
                |
                | Lazy Initialized Thread Safe References:
                | ${
                    threadSafeLazyInitializedFieldReferences
                        .map(_+" | Lazy Initialized Thread Safe Field Reference")
                        .mkString("\n")
                }
                |
                | Immutable References:
                | ${
                    immutableReferences
                        .map(_+" | immutable field Reference")
                        .mkString("\n")
                }
                |
                |""".stripMargin
            )
        }

        val fieldGroupedResults = propertyStore
            .entities(FieldImmutability.key)
            .filter(_.isFinal)
            .filter(eps ⇒ allFieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]))
            .toTraversable
            .groupBy(_.asFinal.p)

        val mutableFields = fieldGroupedResults
            .getOrElse(MutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val shallowImmutableFields = fieldGroupedResults
            .getOrElse(ShallowImmutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val dependentImmutableFields = fieldGroupedResults
            .getOrElse(DependentImmutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val deepImmutableFields = fieldGroupedResults
            .getOrElse(DeepImmutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        if (analysis == All || analysis == Fields) {
            stringBuilderResults.append(
                s"""
                | Mutable Fields:
                | ${mutableFields.map(_+" | Mutable Field ").mkString("\n")}
                |
                | Shallow Immutable Fields:
                | ${shallowImmutableFields.map(_+" | Shallow Immutable Field ").mkString("\n")}
                |
                | Dependent Immutable Fields:
                | ${
                    dependentImmutableFields
                        .map(_+" | Dependent Immutable Field ")
                        .mkString("\n")
                }
                |
                | Deep Immutable Fields:
                | ${deepImmutableFields.map(_+" | Deep Immutable Field ").mkString("\n")}
                |
                |""".stripMargin
            )
        }

        val classGroupedResults = propertyStore
            .entities(ClassImmutability.key)
            .filter(_.isFinal)
            .filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType]))
            .toTraversable
            .groupBy(_.asFinal.p)

        def unpackClass(eps: EPS[Entity, OrderedProperty]): String = {
            val classFile = eps.e.asInstanceOf[ObjectType]
            val className = classFile.simpleName
            s"${classFile.packageName.replace("/", ".")}.$className"
        }

        val mutableClasses =
            classGroupedResults
                .getOrElse(MutableClass, Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val shallowImmutableClasses =
            classGroupedResults
                .getOrElse(ShallowImmutableClass, Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val dependentImmutableClasses =
            classGroupedResults
                .getOrElse(DependentImmutableClass, Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val deepImmutables = classGroupedResults.getOrElse(DeepImmutableClass, Iterator.empty)

        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val deepImmutableClassesInterfaces = deepImmutables
            .filter(eps ⇒ allInterfaces.contains(eps.e.asInstanceOf[ObjectType]))
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val deepImmutableClasses = deepImmutables
            .filter(eps ⇒ !allInterfaces.contains(eps.e.asInstanceOf[ObjectType]))
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        if (analysis == All || analysis == Classes) {
            stringBuilderResults.append(
                s"""
                | Mutable Classes:
                | ${mutableClasses.map(_+" | Mutable Class ").mkString("\n")}
                |
                | Shallow Immutable Classes:
                | ${shallowImmutableClasses.map(_+" | Shallow Immutable Class ").mkString("\n")}
                |
                | Dependent Immutable Classes:
                | ${
                    dependentImmutableClasses
                        .map(_+" | Dependent Immutable Class ")
                        .mkString("\n")
                }
                |
                | Deep Immutable Classes:
                | ${deepImmutableClasses.map(_+" | Deep Immutable Classes ").mkString("\n")}
                |
                | Deep Immutable Interfaces:
                | ${
                    deepImmutableClassesInterfaces
                        .map(_+" | Deep Immutable Interfaces ")
                        .mkString("\n")
                }
                |""".stripMargin
            )
        }

        val typeGroupedResults = propertyStore
            .entities(TypeImmutability.key)
            .filter(_.isFinal)
            .filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType]))
            .toTraversable
            .groupBy(_.asFinal.p)

        val mutableTypes = typeGroupedResults
            .getOrElse(MutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val shallowImmutableTypes = typeGroupedResults
            .getOrElse(ShallowImmutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val dependentImmutableTypes = typeGroupedResults
            .getOrElse(DependentImmutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val deepImmutableTypes = typeGroupedResults
            .getOrElse(DeepImmutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        if (analysis == All || analysis == Types) {
            stringBuilderResults.append(
                s"""
                | Mutable Types:
                | ${mutableTypes.map(_+" | Mutable Type ").mkString("\n")}
                |
                | Shallow Immutable Types:
                | ${shallowImmutableTypes.map(_+" | Shallow Immutable Types ").mkString("\n")}
                |
                | Dependent Immutable Types:
                | ${dependentImmutableTypes.map(_+" | Dependent Immutable Types ").mkString("\n")}
                |
                | Deep Immutable Types:
                | ${deepImmutableTypes.map(_+" | Deep Immutable Types ").mkString("\n")}
                |""".stripMargin
            )
        }

        val stringBuilderNumber: StringBuilder = new StringBuilder

        if (analysis == FieldReferences || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Mutable References: ${mutableFieldReferences.size}
                | Lazy Initialized Not Thread Safe Field References: ${notThreadSafeLazyInitializedFieldReferences.size}
                | Lazy Initialized Not Thread Safe But Deterministic Field References:
                ${lazyInitializedReferencesNotThreadSafeButDeterministic.size}
                | Lazy Initialized Thread Safe Field Reference: ${threadSafeLazyInitializedFieldReferences.size}
                | Immutable Field References: ${immutableReferences.size}
                | Field References: ${allFieldsInProjectClassFiles.size}
                |""".stripMargin
            )
        }

        if (analysis == Fields || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Mutable Fields: ${mutableFields.size}
                | Shallow Immutable Fields: ${shallowImmutableFields.size}
                | Dependent Immutable Fields: ${dependentImmutableFields.size}
                | Deep Immutable Fields: ${deepImmutableFields.size}
                | Fields: ${allFieldsInProjectClassFiles.size}
                |""".stripMargin
            )
        }

        if (analysis == Classes || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Mutable Classes: ${mutableClasses.size}
                | Shallow Immutable Classes: ${shallowImmutableClasses.size}
                | Dependent Immutable Classes: ${dependentImmutableClasses.size}
                | Deep Immutable Classes: ${deepImmutableClasses.size}
                | Classes: ${allProjectClassTypes.size - deepImmutableClassesInterfaces.size}
                |
                | Deep Immutable Interfaces: ${deepImmutableClassesInterfaces.size}
                |
                |""".stripMargin
            )
        }

        if (analysis == Types || analysis == All)
            stringBuilderNumber.append(
                s"""
                | Mutable Types: ${mutableTypes.size}
                | Shallow Immutable Types: ${shallowImmutableTypes.size}
                | Dependent Immutable Types: ${dependentImmutableTypes.size}
                | Deep immutable Types: ${deepImmutableTypes.size}
                | Types: ${allProjectClassTypes.size}
                |""".stripMargin
            )

        val totalTime = projectTime + callGraphTime + analysisTime

        stringBuilderNumber.append(
            s"""
            | running ${analysis.toString} analysis
            | took:
            |   $totalTime seconds total time
            |   $projectTime seconds project time
            |   $callGraphTime seconds callgraph time
            |   $analysisTime seconds analysis time
            |""".stripMargin
        )

        println(
            s"""
            |
            | ${stringBuilderNumber.toString()}
            |
            | time results:
            |
            | $numThreads Threads :: took $analysisTime seconds analysis time
            |
            | results folder: $resultsFolder
            |
            | CofigurationName: $configurationName
            |
            |  level: ${project.getProjectInformationKeyInitializationData(AIDomainFactoryKey)}
            |
            |  consider escape: ${
                project.config.atKey(
                    "org.opalj.fpcf.analyses.L3FieldImmutabilityAnalysis.considerEscape"
                )
            }
            |  consider genericity: ${
                project.config.atKey(
                    "org.opalj.fpcf.analyses.L3FieldImmutabilityAnalysis.considerGenericity"
                )
            }
            |  consider lazy initialization: ${
                project.config.atKey(
                    "org.opalj.fpcf.analyses.L0FieldReferenceImmutabilityAnalysis.considerLazyInitialization"
                )
            }
            |
            |propertyStore: ${propertyStore.getClass}
            |
            |""".stripMargin
        )

        val fileNameExtension = {
            {
                if (withoutConsiderGenericity) {
                    println("withoutConsiderGenericity")
                    "_withoutConsiderGenericity"
                } else ""
            } + {
                if (withoutConsiderLazyInitialization) {
                    println("withoutConsiderLazyInitialization")
                    "_withoutConsiderLazyInitialization"
                } else ""
            } + {
                if (closedWorldAssumption) {
                    println("closed world assumption")
                    "_closedWorldAssumption_"
                } else ""
            } + {
                if (numThreads == 0) ""
                else s"_${numThreads}threads"
            } + s"_l$level"

        }

        if (resultsFolder != null) {
            import java.text.SimpleDateFormat

            val calender = Calendar.getInstance()
            calender.add(Calendar.ALL_STYLES, 1)
            val date = calender.getTime()
            val simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")

            val file = new File(
                s"$resultsFolder/${configurationName.getOrElse("")}_${times}_"+
                    s"${analysis.toString}_${simpleDateFormat.format(date)}_$fileNameExtension.txt"
            )

            println(s"filepath: ${file.getAbsolutePath}")
            val bw = new BufferedWriter(new FileWriter(file))

            try {
                bw.write(
                    s""" ${stringBuilderResults.toString()}
                    |
                    | ${stringBuilderNumber.toString()}
                    |
                    | level: $level
                    |
                    | jdk folder: $JRELibraryFolder
                    |
                    |""".stripMargin
                )

                bw.close()
            } catch {
                case _: IOException ⇒ println(s"could not write file: ${file.getName}")
            } finally {
                bw.close()
            }

        }

        println(s"propertyStore: ${propertyStore.getClass.toString}")

        println(s"jdk folder: $JRELibraryFolder")

        BasicReport(stringBuilderNumber.toString())
    }

    def main(args: Array[String]): Unit = {

        def usage: String = {
            s"""
            | Usage: Immutability
            | -cp <JAR file/Folder containing class files> OR -JDK
            | -projectDir <project directory>
            | -libDir <library directory>
            | -isLibrary
            | [-JDK] (running with the JDK)
            | [-analysis <imm analysis that should be executed: References, Fields, Classes, Types, All>]
            | [-threads <threads that should be max used>]
            | [-resultFolder <folder for the result files>]
            | [-closedWorld] (uses closed world assumption, i.e. no class can be extended)
            | [-noJDK] (running without the JDK)
            | [-callGraph <CHA|RTA|PointsTo> (Default: RTA)
            | [-level] <0|1|2> (domain level  Default: 2)
            | [-withoutConsiderGenericity]
            | [-withoutConsiderLazyInitialization]
            | [-times <1...n>] (times of execution. n is a natural number)
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
        var callGraphName: Option[String] = None
        var level = 2
        var withoutConsiderLazyInitialization = false
        var withoutConsiderGenericity = false
        var times = 1
        var multiProjects = false
        var configurationName: Option[String] = None

        def readNextArg(): String = {
            i = i + 1
            if (i < args.length) {
                args(i)
            } else {
                println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        var analysis: Analyses = All

        while (i < args.length) {
            args(i) match {

                case "-analysis" ⇒
                    val result = readNextArg()
                    if (result == "All")
                        analysis = All
                    else if (result == "FieldReferences")
                        analysis = FieldReferences
                    else if (result == "Fields")
                        analysis = Fields
                    else if (result == "Classes")
                        analysis = Classes
                    else if (result == "Types")
                        analysis = Types
                    else {
                        println(usage)
                        throw new IllegalArgumentException(s"unknown parameter: $result")
                    }

                case "-threads"                           ⇒ numThreads = readNextArg().toInt
                case "-cp"                                ⇒ cp = new File(readNextArg())
                case "-resultFolder"                      ⇒ resultFolder = FileSystems.getDefault.getPath(readNextArg())
                case "-timeEvaluation"                    ⇒ timeEvaluation = true
                case "-threadEvaluation"                  ⇒ threadEvaluation = true
                case "-projectDir"                        ⇒ projectDir = Some(readNextArg())
                case "-libDir"                            ⇒ libDir = Some(readNextArg())
                case "-closedWorld"                       ⇒ closedWorldAssumption = true
                case "-isLibrary"                         ⇒ isLibrary = true
                case "-noJDK"                             ⇒ withoutJDK = true
                case "-callGraph"                         ⇒ callGraphName = Some(readNextArg())
                case "-level"                             ⇒ level = Integer.parseInt(readNextArg())
                case "-times"                             ⇒ times = Integer.parseInt(readNextArg())
                case "-withoutConsiderGenericity"         ⇒ withoutConsiderGenericity = true
                case "-withoutConsiderLazyInitialization" ⇒ withoutConsiderLazyInitialization = true
                case "-multi"                             ⇒ multiProjects = true
                case "-analysisName"                      ⇒ configurationName = Some(readNextArg())
                case "-JDK" ⇒
                    cp = JRELibraryFolder
                    withoutJDK = true

                case unknown ⇒
                    println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }
        if (!(0 <= level && level <= 2))
            throw new Exception(s"not a domain level: $level")

        val callGraphKey = callGraphName match {
            case Some("CHA")        ⇒ CHACallGraphKey
            case Some("PointsTo")   ⇒ AllocationSiteBasedPointsToCallGraphKey
            case Some("RTA") | None ⇒ RTACallGraphKey
            case Some(a) ⇒
                Console.println(s"unknown call graph analysis: $a")
                Console.println(usage)
                return ;
        }
        var nIndex = 1
        while (nIndex <= times) {
            if (multiProjects) {
                // println("xxxxxxxxxx: "+cp.listFiles().map(_.isDirectory).mkString(", ")) //.filter(_.isDirectory))
                for (subp ← cp.listFiles()) { //.filter(_.isDirectory)) {
                    evaluate(
                        subp,
                        analysis,
                        numThreads,
                        projectDir,
                        libDir,
                        resultFolder,
                        withoutJDK,
                        isLibrary || (subp eq JRELibraryFolder),
                        closedWorldAssumption,
                        callGraphKey,
                        level,
                        withoutConsiderGenericity,
                        withoutConsiderLazyInitialization,
                        configurationName,
                        nIndex
                    )
                }
            } else {
                println(s"start $nIndex")
                evaluate(
                    cp,
                    analysis,
                    numThreads,
                    projectDir,
                    libDir,
                    resultFolder,
                    withoutJDK,
                    isLibrary,
                    closedWorldAssumption,
                    callGraphKey,
                    level,
                    withoutConsiderGenericity,
                    withoutConsiderLazyInitialization,
                    configurationName,
                    nIndex
                )
            }
            nIndex = nIndex + 1
        }
    }
}
