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
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.util.Callback
import javafx.beans.value.ObservableValue
import javafx.scene.layout.Priority

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.Group
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.TableView
import scalafx.scene.control.ProgressBar
import scalafx.scene.control.Label
import scalafx.scene.control.ListView
import scalafx.scene.control.TextArea
import scalafx.scene.control.TableCell
import scalafx.scene.control.Button
import scalafx.scene.image.Image
import scalafx.scene.web.WebView
import scalafx.scene.layout.VBox

import org.controlsfx.control.PopOver
import scalafx.beans.property.IntegerProperty

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
    val featureIDs = featureExtractors.flatMap(fe ⇒ fe.featureIDs.map((_, fe)))
    val projectConfigurations = config.as[List[ProjectConfiguration]]("org.opalj.hermes.projects")

    val data = {
        val data = ObservableBuffer.empty[ProjectFeatures[URL]]
        for { projectConfiguration ← projectConfigurations } {
            val features = featureExtractors map { fe ⇒ (fe, fe.createInitialFeatures[URL]) }
            data += ProjectFeatures(projectConfiguration, features)
        }
        data
    }
    val perFeatureCounts: Array[IntegerProperty] = {
        val perFeatureCounts = Array.fill(featureIDs.length)(IntegerProperty(0))
        data.foreach { projectFeatures ⇒
            projectFeatures.features.view.zipWithIndex.foreach { fi ⇒
                val (feature, index) = fi
                feature.onChange { (_, oldValue, newValue) ⇒
                    val change = newValue.count - oldValue.count
                    if (change != 0)
                        perFeatureCounts(index).value = perFeatureCounts(index).value + change
                }
            }
        }
        perFeatureCounts
    }

    def analyzeCorpus(): Thread = {
        val task = new Runnable {
            def run(): Unit = {
                val totalSteps = (featureExtractors.size * projectConfigurations.size).toDouble
                val stepsDone = new AtomicInteger(0)
                for {
                    // Using an iterator is required to avoid eager initialization of all projects!
                    projectFeatures ← data.toIterator
                    if !Thread.currentThread.isInterrupted()
                    projectConfiguration = projectFeatures.projectConfiguration
                    projectInstantiation = projectConfiguration.instantiate
                    project = projectInstantiation.project
                    rawClassFiles = projectInstantiation.rawClassFiles
                    (featureExtractor, features) ← projectFeatures.featureGroups.par
                    featuresMap = features.map(f ⇒ (f.value.id, f)).toMap
                    if !Thread.currentThread.isInterrupted()
                } {
                    val features = featureExtractor(
                        projectConfiguration,
                        project,
                        rawClassFiles
                    )

                    Platform.runLater {
                        features.foreach { f ⇒ featuresMap(f.id).value = f }
                        val progress = stepsDone.incrementAndGet() / totalSteps
                        if (progressBar.getProgress < progress) {
                            progressBar.setProgress(progress)
                        }
                    }
                }

                Platform.runLater { rootPane.getChildren().remove(progressBar) }
            }
        }
        val t = new Thread(task)
        t.setDaemon(true)
        t.start()
        t
    }

    val progressBar = new ProgressBar { hgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }

    val projectColumn = new TableColumn[ProjectFeatures[URL], String]("Project")
    projectColumn.setCellValueFactory(
        new Callback[CellDataFeatures[ProjectFeatures[URL], String], ObservableValue[String]] {
            def call(p: CellDataFeatures[ProjectFeatures[URL], String]): ObservableValue[String] = {
                p.getValue.id
            }
        }
    )

    val featureColumns = featureIDs.zipWithIndex.map { fid ⇒
        val ((name, extractor), featureIndex) = fid
        val featureColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]("")
        featureColumn.setPrefWidth(60.0d)
        featureColumn.cellValueFactory = { p ⇒ p.getValue.features(featureIndex) }
        featureColumn.cellFactory = { (_) ⇒
            new TableCell[ProjectFeatures[URL], Feature[URL]] {
                var currentValue = 0
                item.onChange { (_, _, newFeature) ⇒
                    text = if (newFeature != null) newFeature.count.toString else ""
                    style =
                        if (newFeature == null)
                            "-fx-background-color: gray"
                        else {
                            currentValue = newFeature.count
                            if (newFeature.count == 0) {
                                if (perFeatureCounts(featureIndex).value == 0)
                                    "-fx-background-color: #ffd0db"
                                else
                                    "-fx-background-color: #ffffe0"
                            } else {
                                "-fx-background-color: #aaffaa"
                            }
                        }
                }
                perFeatureCounts(featureIndex).onChange { (_, oldValue, newValue) ⇒
                    if (oldValue.intValue() != newValue.intValue) {
                        style =
                            if (newValue == 0)
                                "-fx-background-color: #ffd0db"
                            else if (currentValue == 0)
                                "-fx-background-color: #ffffe0"
                            else
                                "-fx-background-color: #aaffaa"
                    }
                }
            }
        }
        val label = new Label(name)
        label.setRotate(-90)
        label.setPadding(Insets(5, 5, 5, 5))
        val button = new Button("Doc.") { hgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }
        val group = new Group(label)
        val box = new VBox(group, button)
        box.setAlignment(Pos.BottomCenter)
        box.hgrow = Priority.ALWAYS
        box.vgrow = Priority.ALWAYS
        box.maxWidth = Double.MaxValue
        featureColumn.setGraphic(box)
        val webView = new WebView
        webView.setPrefSize(300.0d, 400.0d)
        val webEngine = webView.engine
        webEngine.setUserStyleSheetLocation(Queries.CSS)
        webEngine.loadContent(extractor.htmlDescription)
        val popOver = new PopOver(webView)
        popOver.setTitle(extractor.id.replaceAll("\n", " – "))
        popOver.setDetachable(true)
        popOver.setHideOnEscape(false)
        button.onAction = handle { if (!popOver.isShowing()) popOver.show(group) }
        featureColumn
    }

    val tableView =
        new TableView[ProjectFeatures[URL]](data) { columns ++= (projectColumn +: featureColumns) }

    tableView.getSelectionModel.setCellSelectionEnabled(true)
    tableView.getSelectionModel.getSelectedCells.onChange { (positions, _) ⇒
        if (positions.nonEmpty) {
            val position = positions.get(0)
            if (position.getColumn == 0) {
                rootPane.right = new TextArea(projectConfigurations(position.getRow).statistics.mkString("\n"))
            }
        }
    }

    val locationsView = new ListView {
        prefWidth = 200
        minWidth = 150
        maxWidth = 500
    }

    val rootPane = new BorderPane {
        center = tableView
        bottom = progressBar
        right = locationsView
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
