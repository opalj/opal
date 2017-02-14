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
import java.util.prefs.Preferences;

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.csv.CsvFactory

import javafx.scene.control.TableColumn
import javafx.scene.control.SelectionMode
import javafx.scene.layout.Priority
import javafx.stage.Screen

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.stage.Stage
import scalafx.stage.Modality
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
import scalafx.scene.control.Dialog
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.HBox
import scalafx.scene.image.Image
import scalafx.scene.web.WebView
import scalafx.scene.layout.VBox
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.IntegerProperty
import scalafx.scene.control.MenuItem
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.scene.control.MenuBar
import scalafx.scene.control.Menu
import scalafx.scene.input.KeyCombination

import org.controlsfx.control.PopOver
import org.controlsfx.control.HiddenSidesPane
import org.controlsfx.control.PopOver.ArrowLocation

import org.chocosolver.solver.Model
import org.chocosolver.solver.variables.IntVar

import org.opalj.da.ClassFileReader

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * @author Michael Eichberg
 */
object Hermes extends JFXApp {

    val preferences = Preferences.userRoot().node("org.opalj.hermes.Hermes")

    if (parameters.unnamed.size != 1) {
        import Console.err
        err.println("OPAL - Hermes")
        err.println("Invalid parameters. ")
        err.println("The parameter has to be the configuartion which lists a corpus' projects.")
        err.println("java org.opalj.hermes.Hermes <ConfigFile.json>")
        System.exit(1)
    }

    /** Creates the initial, overall configuration. */
    private[this] def initConfig(configFile: File): Config = {
        ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.load())
    }

    /** The base configuration of OPAL and Hermes. */
    val config = initConfig(new File(parameters.unnamed(0)))
    Globals.MaxLocations = config.as[Int](Globals.MaxLocationsKey)

    /** Textual representation of the configuration related to OPAL/Hermes.  */
    def renderConfig: String = {
        config.
            getObject("org.opalj").
            render(ConfigRenderOptions.defaults().setOriginComments(false))
    }

    /** The list of all registered feature queries. */
    val queries: List[Query] = config.as[List[Query]]("org.opalj.hermes.queries")

    /** The list of enabled feature queries. */
    val featureQueries: List[FeatureQuery] = {
        queries.flatMap(q ⇒ if (q.isEnabled) q.reify else None)
    }

    /** The list of unique features derived by enabled feature queries. */
    val featureIDs: List[(String, FeatureQuery)] = {
        featureQueries.flatMap(fe ⇒ fe.featureIDs.map((_, fe)))
    }

    /** The set of all project configurations. */
    val projectConfigurations = config.as[List[ProjectConfiguration]]("org.opalj.hermes.projects")
    // TODO Validate that all project names are unique!

    /** The matrix containing for each project the extensions of all features. */
    private[this] val featureMatrix: ObservableBuffer[ProjectFeatures[URL]] = {
        val featureMatrix = ObservableBuffer.empty[ProjectFeatures[URL]]
        for { projectConfiguration ← projectConfigurations } {
            val features = featureQueries map { fe ⇒ (fe, fe.createInitialFeatures[URL]) }
            featureMatrix += ProjectFeatures(projectConfiguration, features)
        }
        featureMatrix
    }

    /** Summary of the number of occurrences of a feature across all projects. */
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

    /* stable */ private[this] val analysesFinished: BooleanProperty = BooleanProperty(false)

    /**
     * Executes the queries for all projects. Basically, the queries are executed in parallel
     * for each project.
     */
    private[this] def analyzeCorpus(): Thread = {
        val t = new Thread {
            override def run(): Unit = {
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
                    (featureQuery, features) ← projectFeatures.featureGroups.par
                    featuresMap = features.map(f ⇒ (f.value.id, f)).toMap
                    if !Thread.currentThread.isInterrupted()
                } {
                    val features = featureQuery(projectConfiguration, project, rawClassFiles)

                    Platform.runLater {
                        // (implicitly) update the feature matrix
                        features.foreach { f ⇒ featuresMap(f.id).value = f }

                        val progress = stepsDone.incrementAndGet() / totalSteps
                        if (progressBar.getProgress < progress) {
                            progressBar.setProgress(progress)
                        }
                    }
                }

                // we are done with everything
                Platform.runLater {
                    rootPane.getChildren().remove(progressBar)
                    analysesFinished.value = true
                }
            }
        }
        t.setDaemon(true)
        t.start()
        t
    }

    // Must only be called after all features were computed.
    private[this] def computeCorpus(): Unit = {
        // GOAL:  Select projects with an overall minimal number of methods such that every
        //        possible feature occurs at least once.
        //        min sum(p_i + methods of p_i )
        val model = new Model("Project Selection")

        // CONSTRAINTS
        // - [per feature f] sum(p_i which has feature f) > 0
        val pis: Array[IntVar] = featureMatrix.map(pf ⇒ model.boolVar(pf.id.value)).toArray
        perFeatureCounts.iterator.zipWithIndex.foreach { fCount_fIndex ⇒
            val (fCount, fIndex) = fCount_fIndex
            if (fCount.value > 0) {
                val projectsWithFeature = pis.toIterator.zipWithIndex.collect {
                    case (pi, pIndex) if featureMatrix(pIndex).features(fIndex).value.count > 0 ⇒ pi
                }.toArray
                model.sum(projectsWithFeature, ">", 0).post()
            }
        }
        val piSizes: Array[Int] =
            featureMatrix.map(pf ⇒
                pf.projectConfiguration.statistics.getOrElse("ProjectMethods", 0)).toArray
        // OPTIMIZATION GOAL
        val overallSize = model.intVar("objective", 0, IntVar.MAX_INT_BOUND /*=21474836*/ )
        model.scalar(pis, piSizes, "=", overallSize).post()
        model.setObjective(Model.MINIMIZE, overallSize)
        val solver = model.getSolver

        val solutionTextArea = new TextArea() {
            editable = false
            prefHeight = 300
            prefWidth = 450
            vgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
        }
        val solverProgressBar = new ProgressBar() {
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
        }
        val contentNode = new VBox(solutionTextArea, solverProgressBar) {
            padding = Insets(5, 5, 5, 5)
        }
        val dialog = new Dialog[String]() {
            initOwner(stage)
            title = "Computing Optimal Project Selection"
            headerText = "Computes a minimal set of projects which has every possible feature."
            dialogPane().buttonTypes = Seq(ButtonType.OK)
            dialogPane().content = contentNode
            resultConverter = { (_) ⇒ solutionTextArea.text.value }
        }

        @volatile var aborted: Boolean = false
        def computeSolutions(): Unit = {
            implicit val ec = ExecutionContext.global
            val solution: Future[Option[String]] = Future {
                if (solver.solve()) {
                    Some(pis.filter(_.getValue == 1).map(pi ⇒ pi.getName).mkString("\n"))
                } else {
                    None
                }
            }
            solution onSuccess {
                case Some(result) ⇒
                    Platform.runLater { solutionTextArea.text = result }
                    if (!aborted) computeSolutions()
                case None ⇒
                    Platform.runLater { contentNode.getChildren.remove(solverProgressBar) }
            }
        }
        computeSolutions()

        dialog.showAndWait()
        aborted = true // make sure that the computation process "finishes soon"
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
                    popOver.headerAlwaysVisibleProperty().value = true
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

    var primitiveFeatureIndex = 0
    val featureQueryColumns = featureQueries map { fq ⇒
        val featureQueryColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]()

        val featureColumns = fq.featureIDs.map { name ⇒
            val featureIndex = primitiveFeatureIndex
            primitiveFeatureIndex += 1
            val featureColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]("")
            featureColumn.setPrefWidth(70.0d)
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
            val group = new Group(label)
            val box = new VBox(group) {
                vgrow = Priority.ALWAYS
                maxWidth = Double.MaxValue
            }
            box.setAlignment(Pos.BottomLeft)
            featureColumn.setGraphic(box)
            featureColumn
        }
        featureQueryColumn.columns ++= featureColumns
        val fqButton = new Button(fq.id) {
            padding = Insets(0, 0, 0, 0)
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
        }
        val webView = new WebView { prefHeight = 600d; prefWidth = 450d }
        val webEngine = webView.engine
        webEngine.setUserStyleSheetLocation(Queries.CSS)
        webEngine.loadContent(fq.htmlDescription)
        val popOver = new PopOver(webView)
        popOver.arrowLocationProperty.value = ArrowLocation.TOP_CENTER
        popOver.setTitle(fq.id.replaceAll("\n", " – "))
        popOver.setDetachable(true)
        popOver.setHeaderAlwaysVisible(true)
        popOver.setHideOnEscape(false)
        fqButton.onAction = handle { if (!popOver.isShowing()) popOver.show(fqButton) }
        featureQueryColumn.setGraphic(fqButton.delegate)
        featureQueryColumn
    }

    val featuresTableView = new TableView[ProjectFeatures[URL]](featureMatrix) {
        columns ++= (projectColumn +: featureQueryColumns)
    }

    featuresTableView.getSelectionModel.setCellSelectionEnabled(true)
    featuresTableView.getSelectionModel.getSelectedCells.onChange { (positions, _) ⇒
        // TODO Bind the content to the selected observable feature as such
        if (positions.nonEmpty) {
            val position = positions.get(0)
            val index = position.getColumn - 1
            if (index >= 0) {
                locationsView.items.value.clear()
                val extensions = featureMatrix(position.getRow).features(index).value.extensions
                extensions.forall(locationsView.items.value.add)
            }
        }
    }

    val locationsView = new ListView[Location[URL]] {
        prefWidth = 1280d
        minWidth = 150d
        maxWidth = 1280d
        maxHeight = Double.MaxValue
        vgrow = Priority.ALWAYS
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
                // TODO Add support for jars in jars..
                val classFile = ClassFileReader.ClassFile(
                    () ⇒ newLocation.source.openConnection().getInputStream
                )(0)
                webView.engine.loadContent(classFile.toXHTML().toString())
            } catch { case t: Throwable ⇒ t.printStackTrace() }
        }
    }

    val mainPane = new HiddenSidesPane();
    mainPane.setContent(featuresTableView);
    mainPane.setRight(
        new VBox(
        new Label(s"Locations (at most ${Globals.MaxLocations} are shown)") {
            padding = Insets(5, 5, 5, 5)
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
            alignment = Pos.Center
            style = "-fx-background-color: #ccc"
        },
        locationsView
    ) {
        padding = Insets(100, 0, 100, 5)
        vgrow = Priority.ALWAYS
        maxHeight = Double.MaxValue
    }.delegate
    )

    def createFileMenuItems(): List[MenuItem] = {

        val showConfig = new MenuItem("Show Config...") {
            onAction = handle {
                val configurationDetails = new TextArea(renderConfig) {
                    editable = false
                    prefHeight = 600d
                }
                val configurationStage = new Stage {
                    scene = new Scene { title = "Configuration"; root = configurationDetails }
                }
                configurationStage.initOwner(stage)
                configurationStage.initModality(Modality.ApplicationModal);
                configurationStage.showAndWait()
            }
        }
        val csvExport = new MenuItem("Export As CVS...") {
            disable <== analysesFinished.not
            accelerator = KeyCombination.keyCombination("Ctrl +Alt +S")
            onAction = handle {
                val fileChooser = new FileChooser {
                    title = "Open Class File"
                    extensionFilters ++= Seq(new ExtensionFilter("Comma Separated Values", "*.csv"))
                }
                val selectedFile = fileChooser.showSaveDialog(stage)
                if (selectedFile != null) {
                    val csvSchema =
                        featureIDs.
                            foldLeft(CsvSchema.builder().addColumn("Project")) { (schema, feature) ⇒
                                schema.addColumn(feature._1, CsvSchema.ColumnType.NUMBER)
                            }.
                            setUseHeader(true).
                            build()
                    val writer = new BufferedWriter(new FileWriter(selectedFile))
                    val csvGenerator = new CsvFactory().createGenerator(writer)
                    csvGenerator.setSchema(csvSchema)
                    featureMatrix.foreach { pf ⇒
                        csvGenerator.writeStartArray()
                        csvGenerator.writeString(pf.id.value)
                        pf.features.foreach { f ⇒ csvGenerator.writeNumber(f.value.count) }
                        csvGenerator.flush()
                        csvGenerator.writeEndArray()
                    }
                    csvGenerator.close()
                }
            }
        }

        val computeProjectsForCorpus = new MenuItem("Compute Projects for Corpus...") {
            disable <== analysesFinished.not
            onAction = handle { computeCorpus() }
        }
        List(showConfig, csvExport, computeProjectsForCorpus)
    }

    val rootPane = new BorderPane {
        top = new MenuBar {
            useSystemMenuBar = true
            minWidth = 100
            menus = Seq(new Menu("File") { items = createFileMenuItems() })
        }
        center = mainPane
        bottom = progressBar
    }

    stage = new PrimaryStage {
        scene = new Scene {
            stylesheets = List(getClass.getResource("Hermes.css").toExternalForm)
            title = "Hermes - "+parameters.unnamed(0)
            root = rootPane

            analyzeCorpus()
        }
        icons += new Image(getClass.getResource("OPAL-Logo-Small.png").toExternalForm)

        // let's restore the window at its last position
        val primaryScreenBounds = Screen.getPrimary().getVisualBounds()
        x = preferences.getDouble("WindowX", 100)
        y = preferences.getDouble("WindowY", 100)
        width = preferences.getDouble("WindowWidth", primaryScreenBounds.getWidth() - 200)
        height = preferences.getDouble("WindowHeight", primaryScreenBounds.getHeight() - 200)

        onCloseRequest = handle {
            preferences.putDouble("WindowX", stage.getX())
            preferences.putDouble("WindowY", stage.getY())
            preferences.putDouble("WindowWidth", stage.getWidth())
            preferences.putDouble("WindowHeight", stage.getHeight())
        }
    }

}
