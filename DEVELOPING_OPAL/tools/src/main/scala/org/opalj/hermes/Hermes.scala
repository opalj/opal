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
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.beans.value.ObservableValue
import javafx.scene.layout.Priority

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.TableView
import scalafx.scene.control.ProgressBar
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.scene.Group
import scalafx.scene.image.Image
import scalafx.geometry.Insets

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * @author Michael Eichberg
 */
object Hermes extends JFXApp {

    if (parameters.unnamed.size != 1) {
        Console.err.println("OPAL - Hermes")
        Console.err.println("Parameters invalid.")
        Console.err.println("java org.opalj.hermes.Hermes <ConfigFile.json>")
        System.exit(1)
    }

    def initConfig(configFile: File): Config = {
        ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.load())
    }
    val config = initConfig(new File(parameters.unnamed(0)))
    def renderConfig: String = {
        config.
            getObject("org.opalj").
            render(ConfigRenderOptions.defaults().setOriginComments(false))
    }

    val queries = config.as[List[Query]]("org.opalj.hermes.queries")
    val featureExtractors = queries.flatMap(q ⇒ if (q.isEnabled) q.reify else None)
    val featureIDs = featureExtractors.flatMap(fe ⇒ fe.featureIDs)
    val projectConfigurations = config.as[List[ProjectConfiguration]]("org.opalj.hermes.projects")

    val data = {
        val data = ObservableBuffer.empty[ProjectFeatures[URL]]
        for { projectConfiguration ← projectConfigurations } {
            val features = featureExtractors map { fe ⇒ (fe, fe.createFeatures[URL]) }
            data += ProjectFeatures(projectConfiguration, features)
        }
        data
    }

    def analyzeCorpus(): Thread = {
        val t = new Thread(
            new Runnable {
                def run(): Unit = {
                    Console.out.println("Starting analysis of corpus.")

                    val totalSteps = (featureExtractors.size * projectConfigurations.size).toDouble
                    val stepsDone = new AtomicInteger(0)
                    for {
                        // the iterator is required to avoid eager initialization of all projects!
                        projectFeatures ← data.toIterator
                        if !Thread.currentThread.isInterrupted()
                        projectConfiguration = projectFeatures.projectConfiguration
                        projectInstantiation = projectConfiguration.instantiate
                        project = projectInstantiation.project
                        rawClassFiles = projectInstantiation.rawClassFiles
                        (featureExtractor, features) ← projectFeatures.featureGroups.par
                        featuresMap = features.map(f ⇒ (f.id, f)).toMap
                        if !Thread.currentThread.isInterrupted()
                    } {
                        Console.out.println(s"Running query: ${featureExtractor.id}")
                        featureExtractor(
                            projectConfiguration,
                            project,
                            rawClassFiles,
                            featuresMap
                        )

                        Platform.runLater {
                            val progress = stepsDone.incrementAndGet() / totalSteps
                            Console.out.println("Progress: "+progress)
                            if (progressBar.getProgress < progress) {
                                progressBar.setProgress(progress)
                            }
                        }
                    }

                    Platform.runLater { rootPane.getChildren().remove(progressBar) }
                    Console.out.println("Analysis of corpus has finished.")
                }
            }
        )
        t.setDaemon(true)
        t.start()
        t
    }

    val progressBar =
        new ProgressBar {
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
        }

    val rootPane = new BorderPane {
        center =
            new TableView[ProjectFeatures[URL]](data) {
                val projectColumn = new TableColumn[ProjectFeatures[URL], String]("Project")
                projectColumn.setCellValueFactory(
                    new Callback[CellDataFeatures[ProjectFeatures[URL], String], ObservableValue[String]]() {
                        def call(p: CellDataFeatures[ProjectFeatures[URL], String]): ObservableValue[String] = {
                            p.getValue.id
                        }
                    }
                )
                val featureColumns = featureIDs.zipWithIndex.map { fid ⇒
                    val (name, index) = fid
                    val featureColumn = new TableColumn[ProjectFeatures[URL], Int]("")
                    featureColumn.setPrefWidth(60.0d)
                    featureColumn.setCellValueFactory(
                        new Callback[CellDataFeatures[ProjectFeatures[URL], Int], ObservableValue[Int]]() {
                            def call(p: CellDataFeatures[ProjectFeatures[URL], Int]): ObservableValue[Int] = {
                                p.getValue.features(index).count
                            }
                        }
                    )
                    //featureColumn.a handle{println("dfdf")}
                    val vbox = new VBox(new Label(name))
                    vbox.setRotate(-90)
                    vbox.setPadding(Insets(5, 5, 5, 5));
                    featureColumn.setGraphic(new Group(vbox))
                    featureColumn
                }
                columns ++= (projectColumn +: featureColumns)
            }
        bottom = progressBar
    }

    stage = new PrimaryStage {
        scene = new Scene {
            stylesheets = List(getClass.getResource("Hermes.css").toExternalForm)
            title = "Hermes"
            root = rootPane

            analyzeCorpus()
        }
        icons += new Image(getClass.getResource("OPAL-Logo.png").toExternalForm)
    }

}
