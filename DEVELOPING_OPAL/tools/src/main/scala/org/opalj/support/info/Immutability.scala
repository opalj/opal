/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.language.postfixOps

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.immutable.SortedSet

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.flagConverter
import org.rogach.scallop.stringConverter

import org.opalj.ai.common.DomainArg
import org.opalj.br.ClassType
import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.bytecode.JDKArg
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.cli.ChoiceArg
import org.opalj.cli.ConfigurationNameArg
import org.opalj.cli.LibraryArg
import org.opalj.cli.OutputDirArg
import org.opalj.cli.ParsedArg
import org.opalj.cli.PlainArg
import org.opalj.cli.ThreadsNumArg
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Determines the assignability of fields and the immutability of fields, classes, and types and provides several
 * setting options for evaluation.
 *
 * @author Tobias Roth
 */
object Immutability extends ProjectsAnalysisApplication {

    protected class ImmutabilityConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {

        val description = "Compute information on the immutability of fields, classes, and types"

        private val analysisArg = new ParsedArg[String, Analyses] with ChoiceArg[Analyses] {
            override val name: String = "analysis"
            override val description: String = "The analysis that should be executed"
            override val choices: Seq[String] = Seq("FieldAssignability", "Fields", "Classes", "Types", "All")
            override val defaultValue: Option[String] = Some("All")

            override def parse(arg: String): Analyses = arg match {
                case "All"                => All
                case "FieldAssignability" => Assignability
                case "Fields"             => Fields
                case "Classes"            => Classes
                case "Types"              => Types
            }
        }

        private val ignoreLazyInitializationArg = new PlainArg[Boolean] {
            override val name: String = "ignoreLazyInit"
            override val description: String = "Do not consider lazy initialization of fields"
            override val defaultValue: Option[Boolean] = Some(false)

            override def apply(config: Config, value: Option[Boolean]): Config = {
                config.withValue(
                    "org.opalj.fpcf.analyses.L3FieldAssignabilityAnalysis.considerLazyInitialization",
                    ConfigValueFactory.fromAnyRef(!value.get)
                )
            }
        }

        args(
            analysisArg !,
            ignoreLazyInitializationArg !,
            ConfigurationNameArg !
        )
        init()

        val analysis: Analyses = apply(analysisArg)
        val ignoreLazyInitialization: Boolean = apply(ignoreLazyInitializationArg)
    }

    sealed trait Analyses
    private case object Assignability extends Analyses
    private case object Fields extends Analyses
    private case object Classes extends Analyses
    private case object Types extends Analyses
    private case object All extends Analyses

    protected type ConfigType = ImmutabilityConfig

    protected def createConfig(args: Array[String]): ImmutabilityConfig = new ImmutabilityConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ImmutabilityConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {

        var analysisTime: Seconds = Seconds.None

        val (project, projectTime) = analysisConfig.setupProject(cp)
        val (propertyStore, propertyStoreTime) = analysisConfig.setupPropertyStore(project)
        val (_, callGraphTime) = analysisConfig.setupCallGaph(project)

        val allProjectClassTypes = project.allProjectClassFiles.iterator.map(_.thisType).toSet

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap {
            _.fields
        }.toSet

        val dependencies: List[FPCFAnalysisScheduler[_]] =
            List(
                EagerFieldAccessInformationAnalysis,
                LazyL2FieldAssignabilityAnalysis,
                LazyFieldImmutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )
        time {
            project.get(FPCFAnalysesManagerKey).runAll(
                dependencies,
                {
                    (css: List[ComputationSpecification[FPCFAnalysis]]) =>
                        analysisConfig.analysis match {
                            case Assignability =>
                                if (css.contains(LazyL2FieldAssignabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore
                                            .force(f, FieldAssignability.key)
                                    )
                            case Fields =>
                                if (css.contains(LazyFieldImmutabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore
                                            .force(f, FieldImmutability.key)
                                    )
                            case Classes =>
                                if (css.contains(LazyClassImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, ClassImmutability.key))
                            case Types =>
                                if (css.contains(LazyTypeImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, TypeImmutability.key))
                            case All =>
                                if (css.contains(LazyL2FieldAssignabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f => {
                                        import org.opalj.br.fpcf.properties.immutability
                                        propertyStore.force(f, immutability.FieldAssignability.key)
                                    })
                                if (css.contains(LazyFieldImmutabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore
                                            .force(f, FieldImmutability.key)
                                    )
                                if (css.contains(LazyClassImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => {
                                        import org.opalj.br.fpcf.properties.immutability
                                        propertyStore.force(c, immutability.ClassImmutability.key)
                                    })
                                if (css.contains(LazyTypeImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, TypeImmutability.key))
                        }
                }
            )
        } { t => analysisTime = t.toSeconds }

        val stringBuilderResults: StringBuilder = new StringBuilder()

        def unpackField(eps: EPS[Entity, OrderedProperty]): String = {
            val field = eps.e.asInstanceOf[Field]
            val packageName = field.classFile.thisType.packageName.replace("/", ".")
            val className = field.classFile.thisType.simpleName
            val fieldName = field.name
            s"$packageName.$className.$fieldName"
        }

        def unpackAndSort[P <: OrderedProperty](
            results: IterableOnce[EPS[Entity, P]],
            unpack:  EPS[Entity, OrderedProperty] => String
        ): Seq[String] = {
            results.iterator.toSeq.map(unpack).sortWith(_ < _)
        }

        def getByResult[P <: OrderedProperty](
            results: Map[P, Iterable[EPS[Entity, P]]],
            unpack:  EPS[Entity, OrderedProperty] => String,
            kind:    P
        ): Seq[String] = {
            unpackAndSort(results.getOrElse(kind, Iterator.empty), unpack)
        }

        def filteredResults[P <: Property, T](
            kind:     PropertyKey[P],
            entities: Set[T]
        ): Map[P, Iterable[EPS[Entity, P]]] = {
            propertyStore
                .entities(kind)
                .filter(eps => entities.contains(eps.e.asInstanceOf[T]))
                .iterator.to(Iterable)
                .groupBy {
                    _.asFinal.p match {
                        case DependentlyImmutableField(_) => DependentlyImmutableField(SortedSet.empty).asInstanceOf[P]
                        case DependentlyImmutableClass(_) => DependentlyImmutableClass(SortedSet.empty).asInstanceOf[P]
                        case DependentlyImmutableType(_)  => DependentlyImmutableType(SortedSet.empty).asInstanceOf[P]
                        case default                      => default
                    }
                }
        }

        val assignabilityResults = filteredResults(FieldAssignability.key, allFieldsInProjectClassFiles)

        val assignableFields = getByResult(assignabilityResults, unpackField, Assignable)
        val unsafelyInitializedFields = getByResult(assignabilityResults, unpackField, UnsafelyLazilyInitialized)
        val lazilyInitializedFields = getByResult(assignabilityResults, unpackField, LazilyInitialized)
        val effectivelyNonAssignableFields = getByResult(assignabilityResults, unpackField, EffectivelyNonAssignable)
        val nonAssignableFields = getByResult(assignabilityResults, unpackField, NonAssignable)

        if (analysisConfig.analysis == All || analysisConfig.analysis == Assignability) {
            stringBuilderResults.append(
                s"""
                   | Assignable Fields:
                   | ${assignableFields.map(_ + " | Assignable Field ").mkString("\n")}
                   |
                   | Lazy Initialized Not Thread Safe Field:
                   | ${unsafelyInitializedFields.map(_ + " | Lazy Initialized Not Thread Safe Field").mkString("\n")}
                   |
                   | Lazy Initialized Thread Safe Field:
                   | ${lazilyInitializedFields.map(_ + " | Lazy Initialized Thread Safe Field").mkString("\n")}
                   |
                   |
                   | effectively non assignable Fields:
                   | ${effectivelyNonAssignableFields.map(_ + " | effectively non assignable ").mkString("\n")}

                   | non assignable Fields:
                   | ${nonAssignableFields.map(_ + " | non assignable").mkString("\n")}
                   |
                   |""".stripMargin
            )
        }

        val fieldResults = filteredResults(FieldImmutability.key, allFieldsInProjectClassFiles)

        val mutableFields = getByResult(fieldResults, unpackField, MutableField)
        val nonTransitivelyImmutableFields = getByResult(fieldResults, unpackField, NonTransitivelyImmutableField)
        val dependentFields = getByResult(fieldResults, unpackField, DependentlyImmutableField(SortedSet.empty))
        val transitivelyImmutableFields = getByResult(fieldResults, unpackField, TransitivelyImmutableField)

        if (analysisConfig.analysis == All || analysisConfig.analysis == Fields) {
            stringBuilderResults.append(
                s"""
                   | Mutable Fields:
                   | ${mutableFields.map(_ + " | Mutable Field ").mkString("\n")}
                   |
                   | Non Transitively Immutable Fields:
                   | ${nonTransitivelyImmutableFields.map(_ + " | Non Transitively Immutable Field ").mkString("\n")}
                   |
                   | Dependently Immutable Fields:
                   | ${dependentFields.map(_ + " | Dependently Immutable Field ").mkString("\n")}
                   |
                   | Transitively Immutable Fields:
                   | ${transitivelyImmutableFields.map(_ + " | Transitively Immutable Field ").mkString("\n")}
                   |
                   |""".stripMargin
            )
        }

        def unpackClass(eps: EPS[Entity, OrderedProperty]): String = {
            val classFile = eps.e.asInstanceOf[ClassType]
            val className = classFile.simpleName
            s"${classFile.packageName.replace("/", ".")}.$className"
        }

        val classResults = filteredResults(ClassImmutability.key, allProjectClassTypes)

        val mutableClasses = getByResult(classResults, unpackClass, MutableClass)
        val nonTransitivelyImmutableClasses = getByResult(classResults, unpackClass, NonTransitivelyImmutableClass)
        val dependentClasses = getByResult(classResults, unpackClass, DependentlyImmutableClass(SortedSet.empty))
        val transitivelyImmutables = classResults.getOrElse(TransitivelyImmutableClass, Iterator.empty)

        val allInterfaces = project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val transitivelyImmutableClassesInterfaces = unpackAndSort(
            transitivelyImmutables.filter(eps => allInterfaces.contains(eps.e.asInstanceOf[ClassType])),
            unpackClass
        )
        val transitivelyImmutableClasses = unpackAndSort(
            transitivelyImmutables.filter(eps => !allInterfaces.contains(eps.e.asInstanceOf[ClassType])),
            unpackClass
        )

        if (analysisConfig.analysis == All || analysisConfig.analysis == Classes) {
            stringBuilderResults.append(
                s"""
                   | Mutable Classes:
                   | ${mutableClasses.map(_ + " | Mutable Class ").mkString("\n")}
                   |
                   | Non Transitively Immutable Classes:
                   | ${nonTransitivelyImmutableClasses.map(_ + " | Non Transitively Immutable Class ").mkString("\n")}
                   |
                   | Dependently Immutable Classes:
                   | ${dependentClasses.map(_ + " | Dependently Immutable Class ").mkString("\n")}
                   |
                   | Transitively Immutable Classes:
                   | ${transitivelyImmutableClasses.map(_ + " | Transitively Immutable Classes ").mkString("\n")}
                   |
                   | Transitively Immutable Interfaces:
                   | ${
                        transitivelyImmutableClassesInterfaces
                            .map(_ + " | Transitively Immutable Interfaces ")
                            .mkString("\n")
                    }
                   |""".stripMargin
            )
        }

        val typeResults = filteredResults(TypeImmutability.key, allProjectClassTypes)

        val mutableTypes = getByResult(typeResults, unpackClass, MutableType)
        val nonTransitivelyImmutableTypes = getByResult(typeResults, unpackClass, NonTransitivelyImmutableType)
        val dependentTypes = getByResult(typeResults, unpackClass, DependentlyImmutableType(SortedSet.empty))
        val transitivelyImmutableTypes = getByResult(typeResults, unpackClass, TransitivelyImmutableType)

        if (analysisConfig.analysis == All || analysisConfig.analysis == Types) {
            stringBuilderResults.append(
                s"""
                   | Mutable Types:
                   | ${mutableTypes.map(_ + " | Mutable Type ").mkString("\n")}
                   |
                   | Non-Transitively Immutable Types:
                   | ${nonTransitivelyImmutableTypes.map(_ + " | Non-Transitively Immutable Types ").mkString("\n")}
                   |
                   | Dependently Immutable Types:
                   | ${dependentTypes.map(_ + " | Dependently Immutable Types ").mkString("\n")}
                   |
                   | Transitively Immutable Types:
                   | ${transitivelyImmutableTypes.map(_ + " | Transitively Immutable Types ").mkString("\n")}
                   |""".stripMargin
            )
        }

        val stringBuilderNumber: StringBuilder = new StringBuilder

        if (analysisConfig.analysis == Assignability || analysisConfig.analysis == All) {
            stringBuilderNumber.append(
                s"""
                   | Assignable Fields: ${assignableFields.size}
                   | Unsafely Lazily Initialized  Fields: ${unsafelyInitializedFields.size}
                   | lazily Initialized Fields: ${lazilyInitializedFields.size}
                   | Effectively Non Assignable Fields: ${effectivelyNonAssignableFields.size}
                   | Non Assignable Fields: ${nonAssignableFields.size}
                   | Fields: ${allFieldsInProjectClassFiles.size}
                   |""".stripMargin
            )
        }

        if (analysisConfig.analysis == Fields || analysisConfig.analysis == All) {
            val primitiveFields = allFieldsInProjectClassFiles.count { field =>
                !field.fieldType.isReferenceType || field.fieldType == ClassType.String
            }
            stringBuilderNumber.append(
                s"""
                   | Mutable Fields: ${mutableFields.size}
                   | Non Transitively Immutable Fields: ${nonTransitivelyImmutableFields.size}
                   | Dependently Immutable Fields: ${dependentFields.size}
                   | Transitively Immutable Fields: ${transitivelyImmutableFields.size}
                   | Fields: ${allFieldsInProjectClassFiles.size}
                   | Fields with primitive Types / java.lang.String: $primitiveFields
                   |""".stripMargin
            )
        }

        if (analysisConfig.analysis == Classes || analysisConfig.analysis == All) {
            stringBuilderNumber.append(
                s"""
                   | Mutable Classes: ${mutableClasses.size}
                   | Non Transitively Immutable Classes: ${nonTransitivelyImmutableClasses.size}
                   | Dependently Immutable Classes: ${dependentClasses.size}
                   | Transitively Immutable Classes: ${transitivelyImmutableClasses.size}
                   | Classes: ${allProjectClassTypes.size - transitivelyImmutableClassesInterfaces.size}
                   |
                   | Transitively Immutable Interfaces: ${transitivelyImmutableClassesInterfaces.size}
                   |
                   |""".stripMargin
            )
        }

        if (analysisConfig.analysis == Types || analysisConfig.analysis == All)
            stringBuilderNumber.append(
                s"""
                   | Mutable Types: ${mutableTypes.size}
                   | Non Transitively Immutable Types: ${nonTransitivelyImmutableTypes.size}
                   | Dependently Immutable Types: ${dependentTypes.size}
                   | Transitively immutable Types: ${transitivelyImmutableTypes.size}
                   | Types: ${allProjectClassTypes.size}
                   |""".stripMargin
            )

        val totalTime = projectTime + callGraphTime + analysisTime

        stringBuilderNumber.append(
            s"""
               | running ${analysisConfig.analysis} analysis
               | took:
               |   $totalTime seconds total time
               |   $projectTime seconds project time
               |   $propertyStoreTime seconds property store time
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
               | ${analysisConfig(ThreadsNumArg)} Threads :: took $analysisTime seconds analysis time
               |
               | results folder: ${analysisConfig.get(OutputDirArg)}
               |
               | CofigurationName: ${analysisConfig(ConfigurationNameArg)}
               |
               | AI domain: ${analysisConfig(DomainArg)}
               |
               | propertyStore: ${propertyStore.getClass}
               |
               |""".stripMargin
        )

        val domainName = analysisConfig(DomainArg).getClass.getName
        val domainLevel = domainName.substring(domainName.indexOf('.') + 1, domainName.lastIndexOf('.'))
        val fileNameExtension = {
            {
                if (analysisConfig.ignoreLazyInitialization) {
                    println("ignoreLazyInit")
                    "_ignoreLazyInit"
                } else ""
            } + {
                if (analysisConfig(LibraryArg)) {
                    println("is Library")
                    "_isLibrary_"
                } else ""
            } + {
                val numThreads = analysisConfig(ThreadsNumArg)
                if (numThreads == 0) ""
                else s"_${numThreads}threads"
            } + s"_$domainLevel"
        }

        val projectEvalDir = analysisConfig.get(OutputDirArg)
            .map(new File(_, if (analysisConfig(JDKArg).isDefined) "JDK" else cp.head.getName))
        if (projectEvalDir.isDefined) {
            if (!projectEvalDir.get.exists()) projectEvalDir.get.mkdirs()

            val calender = Calendar.getInstance()
            calender.add(Calendar.ALL_STYLES, 1)
            val date = calender.getTime
            val simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")

            val file = new File(
                s"${analysisConfig(OutputDirArg)}/${analysisConfig(ConfigurationNameArg)}_${execution}_" +
                    s"${analysisConfig.analysis}_${simpleDateFormat.format(date)}_$fileNameExtension.txt"
            )

            val bw = new BufferedWriter(new FileWriter(file))

            try {
                bw.write(
                    s""" ${stringBuilderResults.toString()}
                       |
                       | ${stringBuilderNumber.toString()}
                       |
                       | AI domain: ${analysisConfig(DomainArg)}
                       |
                       | jdk folder: $JRELibraryFolder
                       |
                       | callGraph $CallGraphKey
                       |
                       |""".stripMargin
                )

                bw.close()
            } catch {
                case _: IOException => println(s"could not write file: ${file.getName}")
            } finally {
                bw.close()
            }

        }

        (project, BasicReport(stringBuilderNumber.toString()))
    }
}
