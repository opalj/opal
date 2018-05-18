/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package hermes

import java.io.File
import java.net.URL
import java.io.FileWriter
import java.io.BufferedWriter
import java.util.concurrent.atomic.AtomicInteger

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.IntegerProperty
import scalafx.beans.property.LongProperty
import org.opalj.br.analyses.Project

/**
 * Implements the core functionality to evaluate a sef of feature queries against a set of
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
        registeredQueries.flatMap(q ⇒ if (q.isEnabled) q.reify(this) else None)
    }

    /**
     * The list of unique features derived by enabled feature queries; one ''feature query'' may
     * be referenced by multiple unique feature queries.
     */
    lazy val featureIDs: List[(String, FeatureQuery)] = {
        var featureIDs: List[(String, FeatureQuery)] = List.empty

        for {
            featureQuery ← featureQueries
            featureID ← featureQuery.featureIDs
        } {
            if (!featureIDs.exists(_._1 == featureID))
                featureIDs :+= ((featureID, featureQuery))
            else
                throw DuplicateFeatureIDException(
                    featureID,
                    featureQuery,
                    featureIDs.collectFirst { case (`featureID`, fq) ⇒ fq }.get
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
    lazy val featureMatrix: ObservableBuffer[ProjectFeatures[URL]] = {
        val featureMatrix = ObservableBuffer.empty[ProjectFeatures[URL]]
        for { projectConfiguration ← projectConfigurations } {
            val features = featureQueries map { fe ⇒ (fe, fe.createInitialFeatures[URL]) }
            featureMatrix += ProjectFeatures(projectConfiguration, features)
        }
        featureMatrix
    }

    /** Summary of the number of occurrences of a feature across all projects. */
    lazy val perFeatureCounts: Array[IntegerProperty] = {
        val perFeatureCounts = Array.fill(featureIDs.size)(IntegerProperty(0))
        featureMatrix.foreach { projectFeatures ⇒
            projectFeatures.features.view.zipWithIndex foreach { fi ⇒
                val (feature, index) = fi
                feature.onChange { (_, oldValue, newValue) ⇒
                    val change = newValue.count - oldValue.count
                    if (change != 0) {
                        perFeatureCounts(index).value = perFeatureCounts(index).value + change
                    }
                }
            }
        }
        perFeatureCounts
    }

    val analysesFinished: BooleanProperty = BooleanProperty(false)

    // some statistics
    val corpusAnalysisTime: LongProperty = new LongProperty

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
                updateProjectData { projectFeatures.id.value = "! "+projectFeatures.id.value }
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
                    projectFeatures ← featureMatrix.toIterator
                    if !Thread.currentThread.isInterrupted()
                    projectConfiguration = projectFeatures.projectConfiguration
                    projectAnalysisStartTime = System.nanoTime()
                    projectInstantiation = projectConfiguration.instantiate
                    project = projectInstantiation.project
                    rawClassFiles = projectInstantiation.rawClassFiles
                    if isValid(projectFeatures, project, projectAnalysisStartTime)
                    (featureQuery, features) ← projectFeatures.featureGroups.par
                    featuresMap = features.map(f ⇒ (f.value.id, f)).toMap
                    if !Thread.currentThread.isInterrupted()
                } {
                    val featureAnalysisStartTime = System.nanoTime()
                    val features = featureQuery(projectConfiguration, project, rawClassFiles)
                    val featureAnalysisEndTime = System.nanoTime()
                    val featureAnalysisTime = featureAnalysisEndTime - featureAnalysisStartTime

                    reportProgress {
                        featureQuery.accumulatedAnalysisTime.value =
                            featureQuery.accumulatedAnalysisTime.value + featureAnalysisTime
                        corpusAnalysisTime.value = featureAnalysisEndTime - analysesStartTime
                        // (implicitly) update the feature matrix
                        features.foreach { f ⇒ featuresMap(f.id).value = f }

                        stepsDone.incrementAndGet() / totalSteps
                    }
                }

                // we are done with everything
                reportProgress {
                    val analysesEndTime = System.nanoTime()
                    corpusAnalysisTime.value = analysesEndTime - analysesStartTime

                    analysesFinished.value = true
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
    def updateProjectData(f: ⇒ Unit): Unit

    /**
     * Called to report the progress. If the double value is 1.0 the analyses has finished.
     * Note that report progress is executed concurrently, but `f` must not be called
     * concurrently and may need to be scheduled as part of the UI thread if the
     * progress is visualized.
     */
    // Needs to be implemented by subclasses.
    def reportProgress(f: ⇒ Double): Unit

    // ---------------------------------------------------------------------------------------------
    //
    //
    // CONVENIENCE FUNCTIONALITY
    //
    //
    // ---------------------------------------------------------------------------------------------

    def exportStatistics(file: File, exportProjectStatistics: Boolean = true): Unit = {
        io.process(new BufferedWriter(new FileWriter(file))) { writer ⇒
            exportStatistics(writer, exportProjectStatistics)
        }
    }

    def exportStatistics(writer: BufferedWriter, exportProjectStatistics: Boolean): Unit = {
        // Create the set of all names of all project-wide statistics
        var projectStatisticsIDs = Set.empty[String]
        featureMatrix.foreach { pf ⇒
            projectStatisticsIDs ++= pf.projectConfiguration.statistics.keySet
        }

        // Logic to create the csv file:
        val csvSchemaBuilder = CsvSchema.builder().addColumn("Project")
        if (exportProjectStatistics) {
            projectStatisticsIDs.foreach { id ⇒ csvSchemaBuilder.addColumn(id) }
        }
        val csvSchema =
            featureIDs.
                foldLeft(csvSchemaBuilder) { (schema, feature) ⇒
                    schema.addColumn(feature._1, CsvSchema.ColumnType.NUMBER)
                }.
                setUseHeader(true).
                build()

        val csvGenerator = new CsvFactory().createGenerator(writer)
        csvGenerator.setSchema(csvSchema)
        featureMatrix.foreach { pf ⇒
            csvGenerator.writeStartArray()
            csvGenerator.writeString(pf.id.value)
            if (exportProjectStatistics) {
                projectStatisticsIDs.foreach { id ⇒
                    pf.projectConfiguration.statistics.get(id) match {
                        case Some(number) ⇒ csvGenerator.writeNumber(number)
                        case None         ⇒ csvGenerator.writeString("N/A")
                    }
                }
            }
            pf.features.foreach { f ⇒ csvGenerator.writeNumber(f.value.count) }
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
        registeredQueries.iterator.filter(_.isEnabled) foreach { q ⇒
            val fq = q.reify(this).get
            writer.write(q.query)
            writer.write("=")
            writer.write(
                fq.featureIDs.map { fid ⇒
                    fid.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,")
                }.mkString(",")
            )
            writer.newLine()
        }
        writer.flush()
    }

}
