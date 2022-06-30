/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import scala.reflect.io.Directory
import java.io.File
import java.net.URL
import java.io.FileWriter
import java.io.BufferedWriter
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleLongProperty
import org.opalj.br.analyses.Project

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Implements the core functionality to evaluate a set of feature queries against a set of
 * projects; does not provide any UI. The GUI is implemented by the class [[Hermes]] and the
 * command-line interface is implemented by the class [[HermesCLI]].
 *
 * @author Michael Eichberg
 */
trait HermesCore extends HermesConfig {

    // ---------------------------------------------------------------------------------------------
    //
    //
    // STATIC CONFIGURATION
    //
    //
    // ---------------------------------------------------------------------------------------------

    /** The list of all registered feature queries. */
    lazy val registeredQueries: List[Query] = {
        Config.as[List[Query]]("org.opalj.hermes.queries.registered")
    }

    /** The list of enabled feature queries. */
    lazy val featureQueries: List[FeatureQuery] = {
        registeredQueries.flatMap(q => if (q.isEnabled) q.reify(this) else None)
    }

    /**
     * The list of unique features derived by enabled feature queries; one ''feature query'' may
     * be referenced by multiple unique feature queries.
     */
    lazy val featureIDs: List[(String, FeatureQuery)] = {
        var featureIDs: List[(String, FeatureQuery)] = List.empty

        for {
            featureQuery <- featureQueries
            featureID <- featureQuery.featureIDs
        } {
            if (!featureIDs.exists(_._1 == featureID))
                featureIDs :+= ((featureID, featureQuery))
            else
                throw DuplicateFeatureIDException(
                    featureID,
                    featureQuery,
                    featureIDs.collectFirst { case (`featureID`, fq) => fq }.get
                )
        }

        featureIDs
    }

    /** The set of all project configurations. */
    lazy val projectConfigurations: List[ProjectConfiguration] = {
        val pcs = Config.as[List[ProjectConfiguration]]("org.opalj.hermes.projects")
        if (pcs.map(_.id).toSet.size != pcs.size) {
            throw new RuntimeException("some project names are not unique")
        }
        pcs
    }

    // ---------------------------------------------------------------------------------------------
    //
    //
    // FIELDS FOR STORING QUERY(ING RELATED) RESULTS
    //
    //
    // ---------------------------------------------------------------------------------------------

    /** The matrix containing for each project the extensions of all features. */
    lazy val featureMatrix: ObservableList[ProjectFeatures[URL]] = {
        val featureMatrix = FXCollections.observableArrayList[ProjectFeatures[URL]]
        for { projectConfiguration <- projectConfigurations } {
            val features = featureQueries map { fe => (fe, fe.createInitialFeatures[URL]) }
            featureMatrix.add(ProjectFeatures(projectConfiguration, features))
        }
        featureMatrix
    }

    /** Summary of the number of occurrences of a feature across all projects. */
    lazy val perFeatureCounts: Array[IntegerProperty] = {
        val perFeatureCounts =
            Array.fill[IntegerProperty](featureIDs.size)(new SimpleIntegerProperty(0))
        featureMatrix.forEach { projectFeatures =>
            projectFeatures.features.view.zipWithIndex foreach { fi =>
                val (feature, index) = fi
                feature.addListener { (_, oldValue, newValue) =>
                    val change = newValue.count - oldValue.count
                    if (change != 0) {
                        perFeatureCounts(index).setValue(perFeatureCounts(index).getValue + change)
                    }
                }
            }
        }
        perFeatureCounts
    }

    val analysesFinished: BooleanProperty = new SimpleBooleanProperty(false)

    // some statistics
    val corpusAnalysisTime: LongProperty = new SimpleLongProperty

    // ---------------------------------------------------------------------------------------------
    //
    //
    // CORE FUNCTIONALITY
    //
    //
    // ---------------------------------------------------------------------------------------------

    /**
     * Executes the queries for all projects. Basically, the queries are executed in parallel
     * for each project.
     *
     * @note This method is only intended to be called once!
     */
    def analyzeCorpus(runAsDaemons: Boolean): Thread = {

        def isValid(
            projectFeatures:          ProjectFeatures[URL],
            project:                  Project[URL],
            projectAnalysisStartTime: Long
        ): Boolean = {
            if (project.projectClassFilesCount == 0) {
                updateProjectData { projectFeatures.id.setValue("! "+projectFeatures.id.getValue()) }
                false
            } else {
                true
            }
        }

        val analysesStartTime = System.nanoTime()
        val t = new Thread {
            override def run(): Unit = {
                val totalSteps = (featureQueries.size * projectConfigurations.size).toDouble
                val stepsDone = new AtomicInteger(0)
                for {
                    // Using an iterator is required to avoid eager initialization of all projects!
                    projectFeatures <- featureMatrix.iterator.asScala
                    if !Thread.currentThread.isInterrupted()
                    projectConfiguration = projectFeatures.projectConfiguration
                    projectAnalysisStartTime = System.nanoTime()
                    projectInstantiation = projectConfiguration.instantiate
                    project = projectInstantiation.project
                    rawClassFiles = projectInstantiation.rawClassFiles
                    if isValid(projectFeatures, project, projectAnalysisStartTime)
                    (featureQuery, features) <- projectFeatures.featureGroups.par
                    featuresMap = features.map(f => (f.getValue.id, f)).toMap
                    if !Thread.currentThread.isInterrupted()
                } {
                    val featureAnalysisStartTime = System.nanoTime()
                    val features = featureQuery(projectConfiguration, project, rawClassFiles)
                    val featureAnalysisEndTime = System.nanoTime()
                    val featureAnalysisTime = featureAnalysisEndTime - featureAnalysisStartTime

                    reportProgress {
                        featureQuery.accumulatedAnalysisTime.setValue(
                            featureQuery.accumulatedAnalysisTime.getValue + featureAnalysisTime
                        )
                        corpusAnalysisTime.setValue(featureAnalysisEndTime - analysesStartTime)
                        // (implicitly) update the feature matrix
                        features.iterator.foreach { f => featuresMap(f.id).setValue(f) }

                        stepsDone.incrementAndGet() / totalSteps
                    }
                }

                // we are done with everything
                reportProgress {
                    val analysesEndTime = System.nanoTime()
                    corpusAnalysisTime.setValue(analysesEndTime - analysesStartTime)

                    analysesFinished.setValue(true)
                    1.0d // <=> we are done
                }
            }
        }
        t.setDaemon(runAsDaemons)
        t.start()
        t
    }

    /**
     * Note that update project data is executed concurrently, but `f` must not be called
     * concurrently and may need to be scheduled as part of the UI thread if the affected
     * data is visualized.
     */
    def updateProjectData(f: => Unit): Unit

    /**
     * Called to report the progress. If the double value is 1.0 the analyses has finished.
     * Note that report progress is executed concurrently, but `f` must not be called
     * concurrently and may need to be scheduled as part of the UI thread if the
     * progress is visualized.
     */
    // Needs to be implemented by subclasses.
    def reportProgress(f: => Double): Unit

    // ---------------------------------------------------------------------------------------------
    //
    //
    // CONVENIENCE FUNCTIONALITY
    //
    //
    // ---------------------------------------------------------------------------------------------

    def exportStatistics(file: File, exportProjectStatistics: Boolean = true): Unit = {
        io.process(new BufferedWriter(new FileWriter(file))) { writer =>
            exportStatistics(writer, exportProjectStatistics)
        }
    }

    def exportStatistics(writer: BufferedWriter, exportProjectStatistics: Boolean): Unit = {
        // Create the set of all names of all project-wide statistics
        var projectStatisticsIDs = Set.empty[String]
        featureMatrix.forEach { pf =>
            projectStatisticsIDs ++= pf.projectConfiguration.statistics.keySet
        }

        // Logic to create the csv file:
        val csvSchemaBuilder = CsvSchema.builder().addColumn("Project")
        if (exportProjectStatistics) {
            projectStatisticsIDs.foreach { id => csvSchemaBuilder.addColumn(id) }
        }
        val csvSchema =
            featureIDs.
                foldLeft(csvSchemaBuilder) { (schema, feature) =>
                    schema.addColumn(feature._1, CsvSchema.ColumnType.NUMBER)
                }.
                setUseHeader(true).
                build()

        val csvGenerator = new CsvFactory().createGenerator(writer)
        csvGenerator.setSchema(csvSchema)
        featureMatrix.forEach { pf =>
            csvGenerator.writeStartArray()
            csvGenerator.writeString(pf.id.getValue)
            if (exportProjectStatistics) {
                projectStatisticsIDs.foreach { id =>
                    pf.projectConfiguration.statistics.get(id) match {
                        case Some(number) => csvGenerator.writeNumber(number)
                        case None         => csvGenerator.writeString("N/A")
                    }
                }
            }
            pf.features.foreach { f => csvGenerator.writeNumber(f.getValue.count) }
            csvGenerator.flush()
            csvGenerator.writeEndArray()
        }
        csvGenerator.flush()
    }

    /**
     * Exports the mapping between a feature query class and its feature queries.
     *
     * For the feature ids the following substitution scheme is used:
     *  - \\ is replaced by \\\\
     *  - new line ('\\n') is replaced by \\n
     *  - , is replaced by \\,
     *
     * @param file The file to which the mapping will be written.
     */
    def exportMapping(file: File): Unit = {
        io.process(new BufferedWriter(new FileWriter(file))) { exportMapping }
    }

    def exportMapping(writer: BufferedWriter): Unit = {
        registeredQueries.iterator.filter(_.isEnabled) foreach { q =>
            val fq = q.reify(this).get
            writer.write(q.query)
            writer.write("=")
            writer.write(
                fq.featureIDs.map { fid =>
                    fid.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,")
                }.mkString(",")
            )
            writer.newLine()
        }
        writer.flush()
    }

    def exportLocations(dir: Directory): Unit = {
        projectConfigurations.iterator foreach { pc =>
            featureMatrix.forEach { pf =>
                val projectFile = new File(s"${dir.path}/${pf.id.getValue}.tsv")
                io.process(new BufferedWriter(new FileWriter(projectFile))) { writer =>
                    exportLocations(writer, pf)
                }
            }
        }
    }

    def exportLocations(writer: BufferedWriter, pf: ProjectFeatures[URL]): Unit = {
        // Logic to create the csv file:
        val csvSchema = CsvSchema.builder()
            .addColumn("PID")
            .addColumn("FID")
            .addColumn("Source")
            .addColumn("Package")
            .addColumn("FQN")
            .addColumn("MethodName")
            .addColumn("MethodDescriptor")
            .addColumn("PC", CsvSchema.ColumnType.NUMBER)
            .addColumn("Field")
            .setUseHeader(true)
            .setColumnSeparator('\t')
            .build()

        val csvGenerator = new CsvFactory().createGenerator(writer)
        csvGenerator.setSchema(csvSchema)

        def writeEntry[S](
            source:           Option[S],
            pn:               String,
            cls:              String    = "",
            methodName:       String    = "",
            methodDescriptor: String    = "",
            inst:             String    = "",
            field:            String    = ""
        ): Unit = {
            csvGenerator.writeString(source.map(_.toString).getOrElse(""))
            csvGenerator.writeString(pn)
            csvGenerator.writeString(cls)
            csvGenerator.writeString(methodName)
            csvGenerator.writeString(methodDescriptor)
            csvGenerator.writeString(inst)
            csvGenerator.writeString(field)
        }

        val projectId = pf.id.getValue
        pf.features.foreach { f =>
            val feature = f.getValue
            val fid = feature.id
            feature.extensions.foreach { l =>
                csvGenerator.writeStartArray()
                csvGenerator.writeString(projectId)
                csvGenerator.writeString(fid)
                l match {
                    case PackageLocation(source, packageName) =>
                        writeEntry(source, packageName)
                    case ClassFileLocation(source, classFileFQN) =>
                        writeEntry(source, "", s"L${classFileFQN.replace(".", "/")};")
                    case ml @ MethodLocation(cfl, _, _) =>
                        val jvmTypeName = s"L${ml.classFileFQN.replace(".", "/")};"
                        writeEntry(
                            cfl.source,
                            "",
                            jvmTypeName,
                            ml.methodName,
                            ml.methodDescriptor.toJVMDescriptor
                        )
                    case InstructionLocation(ml, pc) =>
                        val jvmTypeName = s"L${ml.classFileFQN.replace(".", "/")};"
                        writeEntry(
                            ml.source,
                            "",
                            jvmTypeName,
                            ml.methodName,
                            ml.methodDescriptor.toJVMDescriptor,
                            pc.toString
                        )
                    case FieldLocation(cfl, fieldName, fieldType) =>
                        val fieldEntry = s"$fieldName : ${fieldType.toJava}"
                        val jvmTypeName = s"L${cfl.classFileFQN.replace(".", "/")};"
                        writeEntry(
                            cfl.source,
                            "",
                            jvmTypeName,
                            field = fieldEntry
                        )
                    case _ => throw new UnknownError(s"unsupported location type: $l")
                }
                csvGenerator.flush()
                csvGenerator.writeEndArray()
            }
        }
        csvGenerator.flush()
    }
}