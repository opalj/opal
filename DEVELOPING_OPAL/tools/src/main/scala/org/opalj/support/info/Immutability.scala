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
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.EagerL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.br.ObjectType
import org.opalj.fpcf.PropertyStore
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
import org.opalj.tac.fpcf.analyses.immutability.EagerL3FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.br.analyses.Project
import org.opalj.log.DevNullLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater
import org.opalj.ai.domain
import org.opalj.log.LogContext
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.AbstractCallGraphKey
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.EPS

/**
 * Determines the immutability of field references, fields, classes and types
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
        cp:                    File,
        analysis:              Analyses,
        numThreads:            Int,
        projectDir:            Option[String],
        libDir:                Option[String],
        resultsFolder:         Path,
        withoutJDK:            Boolean,
        isLibrary:             Boolean,
        closedWorldAssumption: Boolean,
        callGraphKey:          AbstractCallGraphKey,
        level:                 Int,
        reImInferComparison:   Boolean
    ): BasicReport = {

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
            config = config.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
        }

        var projectTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        var callGraphTime: Seconds = Seconds.None

        val project = time {
            Project(classFiles, libFiles ++ JDKFiles, libraryClassFilesAreInterfacesOnly = false, Traversable.empty)
        } { t ⇒ projectTime = t.toSeconds }

        val fieldReferenceDependencies: List[FPCFAnalysisScheduler] = List(
            EagerL0FieldReferenceImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )

        val fieldDependencies: List[FPCFAnalysisScheduler] = List(
            LazyL0FieldReferenceImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            EagerL3FieldImmutabilityAnalysis,
            LazyL1ClassImmutabilityAnalysis,
            LazyL1TypeImmutabilityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )

        val classDepencencies: List[FPCFAnalysisScheduler] = List(
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyL0FieldReferenceImmutabilityAnalysis,
            LazyL3FieldImmutabilityAnalysis,
            LazyL1TypeImmutabilityAnalysis,
            EagerL1ClassImmutabilityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )

        val typeDependencies: List[FPCFAnalysisScheduler] = List(
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyL0FieldReferenceImmutabilityAnalysis,
            LazyL3FieldImmutabilityAnalysis,
            LazyL1ClassImmutabilityAnalysis,
            EagerL1TypeImmutabilityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        )

        val allImmAnalysisDependencies: List[FPCFAnalysisScheduler] =
            List(
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                EagerL0FieldReferenceImmutabilityAnalysis,
                EagerL3FieldImmutabilityAnalysis,
                EagerL1ClassImmutabilityAnalysis,
                EagerL1TypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )

        val dependencies = analysis match {
            case FieldReferences ⇒ fieldReferenceDependencies
            case Fields          ⇒ fieldDependencies
            case Classes         ⇒ classDepencencies
            case Types           ⇒ typeDependencies
            case All             ⇒ allImmAnalysisDependencies
        }

        L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

        var propertyStore: PropertyStore = null

        val analysesManager = project.get(FPCFAnalysesManagerKey)

        println(s"callgraph $callGraphKey")

        time {
            analysesManager.project.get(callGraphKey)
        } { t ⇒ callGraphTime = t.toSeconds }

        println(s"level: $level")

        analysesManager.project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
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
                    org.opalj.fpcf.par.ParTasksManagerConfig.MaxThreads = numThreads
                    // FIXME: this property store is broken
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )

        time {
            propertyStore = analysesManager.runAll(dependencies)._1
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒ analysisTime = t.toSeconds }

        val stringBuilderResults: StringBuilder = new StringBuilder()

        val allProjectClassTypes = project.allProjectClassFiles.toIterator.map(_.thisType).toSet

        val allFieldsInProjectClassFiles = {
            if (reImInferComparison) {
                project.allProjectClassFiles.toIterator.flatMap { _.fields }.
                    filter(f ⇒ !f.isTransient && !f.isSynthetic).toSet
            } else
                project.allProjectClassFiles.toIterator.flatMap { _.fields }.toSet
        }

        val fieldReferenceGroupedResults = propertyStore.entities(FieldReferenceImmutability.key).
            filter(field ⇒ allFieldsInProjectClassFiles.contains(field.e.asInstanceOf[Field])).
            toTraversable.groupBy(_.asFinal.p)

        val fieldReferenceOrder: (EPS[Entity, FieldReferenceImmutability], EPS[Entity, FieldReferenceImmutability]) ⇒ Boolean =
            (epsLeftHandSide, epsRightHandSide) ⇒ epsLeftHandSide.e.toString < epsRightHandSide.e.toString

        val mutableFieldReferences =
            fieldReferenceGroupedResults.getOrElse(MutableFieldReference, Iterator.empty).toSeq.
                sortWith(fieldReferenceOrder)

        val notThreadSafeLazyInitializedFieldReferences =
            fieldReferenceGroupedResults.getOrElse(LazyInitializedNotThreadSafeFieldReference, Iterator.empty).toSeq.
                sortWith(fieldReferenceOrder)

        val lazyInitializedReferencesNotThreadSafeButDeterministic =
            fieldReferenceGroupedResults.
                getOrElse(LazyInitializedNotThreadSafeButDeterministicFieldReference, Iterator.empty).toSeq.
                sortWith(fieldReferenceOrder)

        val threadSafeLazyInitializedFieldReferences =
            fieldReferenceGroupedResults.getOrElse(LazyInitializedThreadSafeFieldReference, Iterator.empty).toSeq.
                sortWith(fieldReferenceOrder)

        val immutableReferences = fieldReferenceGroupedResults.getOrElse(ImmutableFieldReference, Iterator.empty).
            toSeq.sortWith(fieldReferenceOrder)

        if (analysis == All || analysis == FieldReferences) {
            stringBuilderResults.append(
                s"""
                | Mutable References:
                | ${mutableFieldReferences.mkString(" || Mutable Reference \n")}
                |
                | Lazy Initalized Not Thread Safe And Not Deterministic References:
                | ${
                    notThreadSafeLazyInitializedFieldReferences.
                        mkString(" | Lazy Initialized Not Thread Safe And Not Deterministic Reference\n")
                }
                |
                | Lazy Initialized Not Thread Safe But Deterministic References:
                | ${
                    lazyInitializedReferencesNotThreadSafeButDeterministic.
                        mkString(" || Lazy Initialized Not Thread Safe But Deterministic Reference\n")
                }
                |
                | Lazy Initialized Thread Safe References:
                | ${
                    threadSafeLazyInitializedFieldReferences.
                        mkString(" || Lazy Initialized Thread Safe Reference\n")
                }
                |
                | Immutable References:
                | ${immutableReferences.mkString(" || immutable Reference\n")}
                |
                |""".stripMargin
            )
        }

        val fieldGroupedResults = propertyStore.entities(FieldImmutability.key).
            filter(eps ⇒ allFieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field])).
            toTraversable.groupBy(_.asFinal.p)

        val fieldOrder: (EPS[Entity, FieldImmutability], EPS[Entity, FieldImmutability]) ⇒ Boolean =
            (epsLeftHandSide, epsRightHandSide) ⇒ epsLeftHandSide.e.toString < epsRightHandSide.e.toString

        val mutableFields = fieldGroupedResults.getOrElse(MutableField, Iterator.empty).toSeq.sortWith(fieldOrder)

        val shallowImmutableFields = fieldGroupedResults.getOrElse(ShallowImmutableField, Iterator.empty).toSeq.
            sortWith(fieldOrder)

        val dependentImmutableFields = fieldGroupedResults.getOrElse(DependentImmutableField, Iterator.empty).toSeq.
            sortWith(fieldOrder)

        val deepImmutableFields = fieldGroupedResults.getOrElse(DeepImmutableField, Iterator.empty).toSeq.
            sortWith(fieldOrder)

        if (analysis == All || analysis == Fields) {
            stringBuilderResults.append(
                s"""
                | Mutable Fields:
                | ${mutableFields.mkString(" || Mutable Field \n")}
                |
                | Shallow Immutable Fields:
                | ${shallowImmutableFields.mkString(" || Shallow Immutable Field \n")}
                |
                | Dependent Immutable Fields:
                | ${dependentImmutableFields.mkString(" || Dependent Immutable Field \n")}
                |
                | Deep Immutable Fields:
                | ${deepImmutableFields.mkString(" || Deep Immutable Field \n")}
                |
                |""".stripMargin
            )
        }

        //val classGroupedResults = propertyStore.entities(ClassImmutability.key).
        //    filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType])).toTraversable.groupBy(_.p)

        val classGroupedResults = propertyStore.entities(ClassImmutability.key).
            filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType])).toTraversable.groupBy(_.asFinal.p)

        // println(s"cgr size: ${classGroupedResults.size}")
        val order: (EPS[Entity, ClassImmutability], EPS[Entity, ClassImmutability]) ⇒ Boolean =
            (epsLeftHandSide, epsRightHandSide) ⇒ epsLeftHandSide.e.toString < epsRightHandSide.e.toString

        val mutableClasses =
            classGroupedResults.getOrElse(MutableClass, Iterator.empty).toSeq.sortWith(order)

        val shallowImmutableClasses =
            classGroupedResults.getOrElse(ShallowImmutableClass, Iterator.empty).toSeq.sortWith(order)

        val dependentImmutableClasses =
            classGroupedResults.getOrElse(DependentImmutableClass, Iterator.empty).toSeq.sortWith(order)

        val deepImmutables = classGroupedResults.getOrElse(DeepImmutableClass, Iterator.empty)

        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val deepImmutableClassesInterfaces = deepImmutables
            .filter(eps ⇒ allInterfaces.contains(eps.e.asInstanceOf[ObjectType])).toSeq.sortWith(order)

        val deepImmutableClasses = deepImmutables
            .filter(eps ⇒ !allInterfaces.contains(eps.e.asInstanceOf[ObjectType])).toSeq.sortWith(order)

        if (analysis == All || analysis == Classes) {
            stringBuilderResults.append(
                s"""
                | Mutable Classes:
                | ${mutableClasses.mkString(" || Mutable Class \n")}
                |
                | Shallow Immutable Classes:
                | ${shallowImmutableClasses.mkString(" || Shallow Immutable Class \n")}
                |
                | Dependent Immutable Classes:
                | ${dependentImmutableClasses.mkString(" || Dependent Immutable Class \n")}
                |
                | Deep Immutable Classes:
                | ${deepImmutableClasses.mkString(" || Deep Immutable Classes \n")}
                |
                | Deep Immutable Interfaces:
                | ${deepImmutableClassesInterfaces.mkString(" || Deep Immutable Interfaces \n")}
                |""".stripMargin
            )
        }

        // val typeGroupedResults = propertyStore.entities(TypeImmutability.key).
        //     filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType])).toTraversable.groupBy(_.e)

        val typeGroupedResults = propertyStore.entities(TypeImmutability.key).
            filter(eps ⇒ allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType])).toTraversable.groupBy(_.asFinal.p)

        val typeOrder: (EPS[Entity, TypeImmutability], EPS[Entity, TypeImmutability]) ⇒ Boolean =
            (epsLeftHandSide, epsRightHandSide) ⇒ epsLeftHandSide.e.toString < epsRightHandSide.e.toString

        val mutableTypes = typeGroupedResults.getOrElse(MutableType, Iterator.empty).toSeq.
            sortWith(typeOrder)

        val shallowImmutableTypes = typeGroupedResults.getOrElse(ShallowImmutableType, Iterator.empty).toSeq.
            sortWith(typeOrder)

        val dependentImmutableTypes = typeGroupedResults.getOrElse(DependentImmutableType, Iterator.empty).toSeq.
            sortWith(typeOrder)

        val deepImmutableTypes = typeGroupedResults.getOrElse(DeepImmutableType, Iterator.empty).toSeq.
            sortWith(typeOrder)

        if (analysis == All || analysis == Types) {
            stringBuilderResults.append(
                s"""
                | Mutable Types:
                | ${mutableTypes.mkString(" | Mutable Type \n")}
                |
                | Shallow Immutable Types:
                | ${shallowImmutableTypes.mkString(" | Shallow Immutable Types \n")}
                |
                | Dependent Immutable Types:
                | ${dependentImmutableTypes.mkString(" | Dependent Immutable Types \n")}
                |
                | Deep Immutable Types:
                | ${deepImmutableTypes.mkString(" | Deep Immutable Types \n")}
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
                | Deep Immutable Interfaces: ${deepImmutableClassesInterfaces.size}
                | Classes: ${allProjectClassTypes.size}
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
            |""".stripMargin
        )

        if (resultsFolder != null) {
            import java.text.SimpleDateFormat

            val calender = Calendar.getInstance()
            calender.add(Calendar.ALL_STYLES, 1)
            val date = calender.getTime();
            val simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")
            val file = new File(s"$resultsFolder/${analysis.toString}_${simpleDateFormat.format(date)}.txt")

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
                    |"""".stripMargin
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
            | Usage: java …ImmutabilityAnalysisEvaluation
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
            | [-ReImInferComparison] (without transient fields)
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
        var reImInferComparison = false

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
                    else throw new IllegalArgumentException(s"unknown parameter: $result")

                case "-threads"             ⇒ numThreads = readNextArg().toInt
                case "-cp"                  ⇒ cp = new File(readNextArg())
                case "-resultFolder"        ⇒ resultFolder = FileSystems.getDefault.getPath(readNextArg())
                case "-timeEvaluation"      ⇒ timeEvaluation = true
                case "-threadEvaluation"    ⇒ threadEvaluation = true
                case "-projectDir"          ⇒ projectDir = Some(readNextArg())
                case "-libDir"              ⇒ libDir = Some(readNextArg())
                case "-closedWorld"         ⇒ closedWorldAssumption = true
                case "-isLibrary"           ⇒ isLibrary = true
                case "-noJDK"               ⇒ withoutJDK = true
                case "-callGraph"           ⇒ callGraphName = Some(readNextArg())
                case "-level"               ⇒ level = Integer.parseInt(readNextArg())
                case "-ReImInferComparison" ⇒ reImInferComparison = true

                case "-JDK" ⇒
                    cp = JRELibraryFolder
                    withoutJDK = true

                case unknown ⇒
                    Console.println(usage)
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
            reImInferComparison
        )
    }
}

