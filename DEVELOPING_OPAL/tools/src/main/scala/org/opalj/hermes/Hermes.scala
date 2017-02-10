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
import org.controlsfx.control.HiddenSidesPane
import scalafx.beans.property.IntegerProperty
import org.controlsfx.control.PopOver.ArrowLocation
import scalafx.scene.layout.HBox
import javafx.scene.control.SelectionMode
import scalafx.stage.Stage
import org.opalj.da.ClassFileReader

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * @author Michael Eichberg
 */
object Hermes extends JFXApp {

    if (parameters.unnamed.size != 1) {
        import Console.err
        err.println("OPAL - Hermes")
        err.println("Invalid parameters. ")
        err.println("The parameter has to be the configuarion which lists a corpus' projects.")
        err.println("java org.opalj.hermes.Hermes <ConfigFile.json>")
        System.exit(1)
    }

    /** Creates the initial, overall configuration. */
    private[this] def initConfig(configFile: File): Config = {
        ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.load())
    }

    /** The base configuration of OPAL and Hermes. */
    val config = initConfig(new File(parameters.unnamed(0)))

    /**
     * Rendering of the configuration related to OPAL/Hermes.
     */
    def renderConfig: String = {
        config.
            getObject("org.opalj").
            render(ConfigRenderOptions.defaults().setOriginComments(false))
    }

    /** The list of all registered feature queries. */
    val queries: List[Query] = config.as[List[Query]]("org.opalj.hermes.queries")

    /** The list of enabled feature queries. */
    val featureQueries: List[FeatureExtractor] = {
        queries.flatMap(q ⇒ if (q.isEnabled) q.reify else None)
    }

    /** The list of unique features derive by enable feature queries. */
    val featureIDs: List[(String, FeatureExtractor)] = {
        featureQueries.flatMap(fe ⇒ fe.featureIDs.map((_, fe)))
    }

    /** The set of all project configurations. */
    val projectConfigurations = config.as[List[ProjectConfiguration]]("org.opalj.hermes.projects")
    // TODO Validate that all project names are unique!

    /** The matrix containing the results of executing the queries.  */
    private[this] val featureMatrix: ObservableBuffer[ProjectFeatures[URL]] = {
        val featureMatrix = ObservableBuffer.empty[ProjectFeatures[URL]]
        for { projectConfiguration ← projectConfigurations } {
            val features = featureQueries map { fe ⇒ (fe, fe.createInitialFeatures[URL]) }
            featureMatrix += ProjectFeatures(projectConfiguration, features)
        }
        featureMatrix
    }

    /** Count of the number of occurrences of a feature across all projects. */
    val perFeatureCounts: Array[IntegerProperty] = {
        val perFeatureCounts = Array.fill(featureIDs.length)(IntegerProperty(0))
        featureMatrix.foreach { projectFeatures ⇒
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

    /**
     * Executes the queries for all projects. Basically, the queries are executed in parallel
     * for each project.
     */
    def analyzeCorpus(): Thread = {
        val task = new Runnable {
            def run(): Unit = {
                val totalSteps = (featureQueries.size * projectConfigurations.size).toDouble
                val stepsDone = new AtomicInteger(0)
                for {
                    // Using an iterator is required to avoid eager initialization of all projects!
                    projectFeatures ← featureMatrix.toIterator
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
                        // (implicitly) update the feature matrix
                        features.foreach { f ⇒ featuresMap(f.id).value = f }
                        val progress = stepsDone.incrementAndGet() / totalSteps
                        if (progressBar.getProgress < progress) {
                            progressBar.setProgress(progress)
                        }
                    }
                }

                // we are done
                Platform.runLater { rootPane.getChildren().remove(progressBar) }
            }
        }
        val t = new Thread(task)
        t.setDaemon(true)
        t.start()
        t
    }

    //
    //
    // UI SETUP CODE
    //
    //

    val progressBar = new ProgressBar { hgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }

    val projectColumn = new TableColumn[ProjectFeatures[URL], String]("Project")
    // TODO Make the project configuration observable to fill the statistics table automatically.
    projectColumn.cellValueFactory = { p ⇒ p.getValue.id }
    projectColumn.cellFactory = { (column) ⇒
        new TableCell[ProjectFeatures[URL], String] {
            item.onChange { (_, _, newProject) ⇒
                if (newProject != null) {
                    val infoButton = new Button("Info")
                    val popOver = new PopOver()
                    popOver.arrowLocationProperty.value = ArrowLocation.LEFT_CENTER
                    popOver.setTitle(newProject)
                    val textArea = new TextArea("Project statistics are not (yet) available.") {
                        editable = false
                        prefWidth = 300d
                        prefHeight = 300d
                    }
                    popOver.contentNodeProperty.value = textArea
                    popOver.setDetachable(true)
                    popOver.setHideOnEscape(false)
                    infoButton.onAction = handle {
                        if (!popOver.isShowing()) {
                            val statistics =
                                featureMatrix.
                                    find(_.id.value == newProject).get.
                                    projectConfiguration.
                                    statistics
                            textArea.text = statistics.map(e ⇒ e._1+": "+e._2).toList.sorted.mkString("\n")
                            popOver.show(infoButton)
                        }
                    }
                    graphic =
                        new HBox(
                            new Label(newProject) {
                                hgrow = Priority.ALWAYS
                                maxWidth = Double.MaxValue
                                padding = Insets(5, 5, 5, 5)
                            },
                            infoButton
                        )
                }
            }
        }
    }

    val featureColumns = featureIDs.zipWithIndex.map { fid ⇒
        val ((name, extractor), featureIndex) = fid
        val featureColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]("")
        featureColumn.setPrefWidth(60.0d)
        featureColumn.cellValueFactory = { p ⇒ p.getValue.features(featureIndex) }
        featureColumn.cellFactory = { (_) ⇒
            new TableCell[ProjectFeatures[URL], Feature[URL]] {
                var currentValue = -1
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
                    if (oldValue.intValue() != newValue.intValue && currentValue != -1) {
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
        val label = new Label(name) { rotate = -90; padding = Insets(5, 5, 5, 5) }
        val button = new Button("Doc.") { hgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }
        val group = new Group(label)
        val box = new VBox(group, button) { vgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }
        box.setAlignment(Pos.BottomCenter)
        featureColumn.setGraphic(box)
        val webView = new WebView { prefHeight = 400d; prefWidth = 300d }
        val webEngine = webView.engine
        webEngine.setUserStyleSheetLocation(Queries.CSS)
        webEngine.loadContent(extractor.htmlDescription)
        val popOver = new PopOver(webView)
        popOver.arrowLocationProperty.value = ArrowLocation.TOP_CENTER
        popOver.setTitle(extractor.id.replaceAll("\n", " – "))
        popOver.setDetachable(true)
        popOver.setHideOnEscape(false)
        button.onAction = handle { if (!popOver.isShowing()) popOver.show(button) }
        featureColumn
    }

    val featuresTableView = new TableView[ProjectFeatures[URL]](featureMatrix) {
        columns ++= (projectColumn +: featureColumns)
    }

    featuresTableView.getSelectionModel.setCellSelectionEnabled(true)
    featuresTableView.getSelectionModel.getSelectedCells.onChange { (positions, _) ⇒
        // TODO Bind the content to the selected observable feature as such
        if (positions.nonEmpty) {
            val position = positions.get(0)
            val index = position.getColumn - 1
            if (index >= 0) {
                locationsView.items.value.clear()
                featureMatrix(position.getRow).features(index).value.extensions.forall(
                    locationsView.items.value.add
                )
            }
        }
    }

    val locationsView = new ListView[Location[URL]] {
        prefWidth = 1280d
        minWidth = 150d
        maxWidth = 1280d
        padding = Insets(5, 5, 5, 5)
    }
    locationsView.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
    locationsView.getSelectionModel.selectedItem.onChange { (_, _, newLocation) ⇒
        if (newLocation != null) {
            val webView = new WebView
            val stage = new Stage {
                scene = new Scene {
                    title = newLocation.source.toExternalForm()
                    root = webView
                }
                width = 1024
                height = 600
            }
            stage.show();
            try {
                val classFile = ClassFileReader.ClassFile(
                    () ⇒ newLocation.source.openConnection().getInputStream
                )(0)
                webView.engine.loadContent(classFile.toXHTML().toString())
            } catch { case t: Throwable ⇒ t.printStackTrace() }
        }
    }

    val projectPane = new HiddenSidesPane();
    val configurationDetails = new TextArea(renderConfig) {
        editable = false
        prefHeight = 600d
    }
    projectPane.setContent(featuresTableView);
    projectPane.setTop(
        new VBox(
        new Label("Configuration") {
            padding = Insets(5, 5, 5, 5)
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
            alignment = Pos.Center
        },
        configurationDetails
    ) { padding = Insets(5, 5, 5, 5) }.delegate
    )
    projectPane.setRight(
        new VBox(
        new Label("Locations") {
            padding = Insets(5, 5, 5, 5)
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
            alignment = Pos.Center
        },
        locationsView
    ) { padding = Insets(5, 5, 5, 5) }.delegate
    )

    val rootPane = new BorderPane {
        center = projectPane
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
