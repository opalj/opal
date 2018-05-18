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
import java.util.prefs.Preferences
import java.util.Comparator

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.io.Source

import org.chocosolver.solver.Model
import org.chocosolver.solver.variables.IntVar
import javafx.stage.Screen
import javafx.scene.control.TableColumn
import javafx.scene.control.SelectionMode
import javafx.scene.layout.Priority

import org.controlsfx.control.PopOver
import org.controlsfx.control.HiddenSidesPane
import org.controlsfx.control.PopOver.ArrowLocation

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.application.JFXApp
import scalafx.application.Platform
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.scene.Scene
import scalafx.scene.Group
import scalafx.scene.image.Image
import scalafx.scene.web.WebView
import scalafx.scene.input.KeyCombination
import scalafx.scene.layout.VBox
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.StackPane
import scalafx.scene.chart.XYChart
import scalafx.scene.chart.CategoryAxis
import scalafx.scene.chart.NumberAxis
import scalafx.scene.chart.BarChart
import scalafx.scene.chart.PieChart
import scalafx.scene.control.MenuItem
import scalafx.scene.control.TextArea
import scalafx.scene.control.ProgressBar
import scalafx.scene.control.ButtonType
import scalafx.scene.control.Dialog
import scalafx.scene.control.TableCell
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.CheckBox
import scalafx.scene.control.TableView
import scalafx.scene.control.ListView
import scalafx.scene.control.ChoiceDialog
import scalafx.scene.control.MenuBar
import scalafx.scene.control.Menu

import org.opalj.da.ClassFileReader
import org.opalj.util.Nanoseconds
import org.opalj.io.processSource

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * ([[https://bitbucket.org/delors/opal/src/HEAD/DEVELOPING_OPAL/tools/src/main/resources/org/opalj/hermes/Hermes.txt?at=develop see Hermes.txt for further details]]).
 *
 * @author Michael Eichberg
 * @author Christian Schaarschmidt (JavaFX Data Visualization)
 */
object Hermes extends JFXApp with HermesCore {

    final val usage = {
        processSource(Source.fromInputStream(this.getClass.getResourceAsStream("Hermes.txt"))) { s ⇒
            s.getLines().mkString("\n")
        }
    }

    if (parameters.unnamed.size != 1 ||
        parameters.named.size > 1 || (
            parameters.named.size == 1 && parameters.named.get("csv").isEmpty
        )) {
        import Console.err
        err.println("Invalid parameters: "+parameters.named.mkString("{", ",", "}"))
        err.println(usage)
        System.exit(1)
    }

    initialize(new File(parameters.unnamed(0)))

    if (parameters.named.size == 1) {
        // when the statistics were requested, we perform the analysis and then finish Hermes
        analysesFinished onChange { (_, _, isFinished) ⇒
            if (isFinished) {
                exportStatistics(new File(parameters.named("csv")))
                stage.close() // <=> quit Hermes
            }
        }
    }

    // We use standard preferences for saving the application state; not for
    // permanent configuration settings!
    val preferences: Preferences = Preferences.userRoot().node("org.opalj.hermes.Hermes")

    override def updateProjectData(f: ⇒ Unit): Unit = Platform.runLater {
        // We have to ensure that we are not calling this too often to avoid that the
        // UI starts to lag
        f
    }

    override def reportProgress(f: ⇒ Double): Unit = Platform.runLater {
        val progress = f
        if (progress >= 1.0d) rootPane.getChildren.remove(progressBar)
    }

    // Must only be called after all features were computed.
    private[this] def computeCorpus(): Unit = {
        // GOAL:  Select projects with an overall minimal number of methods such that every
        //        possible feature occurs at least once.
        //        min sum(p_i * <methods of p_i> )
        val model = new Model("Project Selection")

        // CONSTRAINTS
        // - [per feature f] sum(p_i which has feature f) > 0
        val releventProjectFeatures =
            featureMatrix.filter { pf ⇒
                pf.projectConfiguration.statistics.getOrElse("ProjectMethods", 0.0d) >= 1.0d
            }.toArray
        val pis: Array[IntVar] = releventProjectFeatures.map(pf ⇒ model.boolVar(pf.id.value))
        perFeatureCounts.iterator.zipWithIndex foreach { fCount_fIndex ⇒
            val (fCount, fIndex) = fCount_fIndex
            if (fCount.value > 0) {
                val projectsWithFeature = pis.toIterator.zipWithIndex.collect {
                    case (pi, pIndex) if featureMatrix(pIndex).features(fIndex).value.count > 0 ⇒ pi
                }.toArray
                model.sum(projectsWithFeature, ">", 0).post()
            }
        }
        val piSizes: Array[Int] =
            releventProjectFeatures.map(_.projectConfiguration.statistics("ProjectMethods").toInt)
        // OPTIMIZATION GOAL
        val overallSize = model.intVar("objective", 0, IntVar.MAX_INT_BOUND /*=21474836*/ )
        model.scalar(pis, piSizes, "=", overallSize).post()
        model.setObjective(Model.MINIMIZE, overallSize)
        val solver = model.getSolver

        val solutionTextArea = new TextArea {
            editable = false
            prefHeight = 300
            prefWidth = 450
            vgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
        }
        val solverProgressBar = new ProgressBar {
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
            solution.onComplete {
                case scala.util.Success(Some(result)) ⇒
                    Platform.runLater { solutionTextArea.text = result }
                    if (!aborted) computeSolutions()
                case scala.util.Success(None) ⇒
                    Platform.runLater { contentNode.getChildren.remove(solverProgressBar) }

                case scala.util.Failure(e) ⇒
                    Platform.runLater {
                        solutionTextArea.text = "Computation failed:\n"+e.getMessage
                        contentNode.getChildren.remove(solverProgressBar)
                    }
            }
        }
        computeSolutions()

        dialog.showAndWait()
        // Make sure that – when the dialog has been closed while we are still computing solutions -
        // the computation process "finishes soon".
        aborted = true
    }

    // ---------------------------------------------------------------------------------------------
    //
    //
    // UI SETUP CODE
    //
    //
    // ---------------------------------------------------------------------------------------------

    private[this] val progressBar = new ProgressBar { hgrow = Priority.ALWAYS; maxWidth = Double.MaxValue }

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
                            val projectFeatures = featureMatrix.find(_.id.value == newProject).get
                            val projectConfiguration = projectFeatures.projectConfiguration
                            val projectStatistics = projectConfiguration.statistics
                            val statistics = projectStatistics.map(e ⇒ f"${e._1}%-32s${e._2}%12.2f")
                            textArea.text = statistics.toList.sorted.mkString("\n")
                            popOver.show(infoButton)
                        }
                    }
                    val isBrokenProject = newProject.startsWith("! ︎")
                    graphic =
                        new HBox(
                            new Label(newProject) {
                                hgrow = Priority.ALWAYS
                                maxWidth = Double.MaxValue
                                padding = Insets(5, 5, 5, 5)
                                if (isBrokenProject)
                                    style = "-fx-background-color: red; -fx-text-fill: white;"
                            },
                            infoButton
                        )
                }
            }
        }
    }

    private val onlyShowNotAvailableFeatures = new CheckBox("Only Show Not Available Features") {
        padding = Insets(5, 5, 5, 5)
        hgrow = Priority.ALWAYS
        maxWidth = Double.MaxValue
        alignment = Pos.Center
        style = "-fx-background-color: #dddddd"
    }

    val showFeatureQueryResults = new GridPane {
        padding = Insets(5, 5, 5, 5)
        hgrow = Priority.ALWAYS
        maxWidth = Double.MaxValue
        alignment = Pos.Center
        style = "-fx-background-color: #eeefdd"
    }

    var primitiveFeatureIndex = 0
    val featureComparator = new Comparator[Feature[URL]] {
        def compare(f1: Feature[URL], f2: Feature[URL]): Int = f1.count - f2.count
    }
    val featureQueryColumns = featureQueries.zipWithIndex map { fqi ⇒
        val (fq, featureQueryIndex) = fqi
        val featureQueryColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]()
        val showFeatureQuery = new CheckBox(fq.id) {
            selected = true
            disable <== onlyShowNotAvailableFeatures.selected
            padding = Insets(15, 15, 15, 15)
        }
        onlyShowNotAvailableFeatures.selected.onChange { (_, _, isSelected) ⇒
            if (!isSelected) showFeatureQuery.selected = true
        }
        showFeatureQueryResults.add(showFeatureQuery, featureQueryIndex % 5, featureQueryIndex / 5)
        featureQueryColumn.visible <== showFeatureQuery.selected

        val featureColumns = fq.featureIDs.map { name ⇒
            val featureIndex = primitiveFeatureIndex
            primitiveFeatureIndex += 1
            val featureColumn = new TableColumn[ProjectFeatures[URL], Feature[URL]]("")
            /* This creates a weird propagation problem due to a bidirectional binding..
            featureColumn.visible <== scalafx.beans.binding.Bindings.createBooleanBinding(
                () ⇒ {
                    !onlyShowNotAvailableFeatures.selected.value ||
                        perFeatureCounts(featureIndex).value == 0
                },
                onlyShowNotAvailableFeatures.selected,
                perFeatureCounts(featureIndex)
            )*/
            perFeatureCounts(featureIndex) onChange { (_, oldCount, newCount) ⇒
                if (newCount != null) {
                    if (oldCount.intValue == 0 &&
                        newCount.intValue > 0 &&
                        onlyShowNotAvailableFeatures.selected.value) {
                        featureColumn.visible = false
                    }
                }
            }
            onlyShowNotAvailableFeatures.selected onChange { (_, _, isSelected) ⇒
                if (isSelected && perFeatureCounts(featureIndex).value > 0) {
                    featureColumn.visible = false
                } else {
                    featureColumn.visible = true
                }
            }
            featureColumn.setComparator(featureComparator)
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
                        if (oldValue.intValue != newValue.intValue && currentValue != -1) {
                            style =
                                if (newValue.intValue == 0)
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
        val webView = new WebView { prefHeight = 600d; prefWidth = 600d }
        val webEngine = webView.engine
        fq.htmlDescription match {
            case Left(page) ⇒
                webEngine.setUserStyleSheetLocation(FeatureQueries.MDCSS.toExternalForm)
                webEngine.loadContent(page)
            case Right(url) ⇒
                webEngine.load(url.toExternalForm)
        }
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
        if (newLocation != null && newLocation.source.isDefined) {
            val webView = new WebView
            val stage = new Stage {
                scene = new Scene {
                    title = newLocation.source.get.toExternalForm()
                    root = webView
                }
                width = 1024
                height = 600
            }
            stage.show();
            try {
                // TODO Add support for jars in jars..
                val classFile = ClassFileReader.ClassFile(
                    () ⇒ newLocation.source.get.openConnection().getInputStream
                )(0)
                webView.engine.loadContent(classFile.toXHTML(newLocation.source).toString())
            } catch { case t: Throwable ⇒ t.printStackTrace() }
        }
    }

    val mainPane = new HiddenSidesPane();
    mainPane.setContent(featuresTableView);
    mainPane.setRight(
        new VBox(
            new Label(s"Locations (at most ${MaxLocations} are shown)") {
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
    mainPane.setTop(
        new VBox(
            new Label(s"Table Configuration") {
                padding = Insets(5, 5, 5, 5)
                hgrow = Priority.ALWAYS
                maxWidth = Double.MaxValue
                alignment = Pos.Center
                style = "-fx-background-color: #ccc"
            },
            onlyShowNotAvailableFeatures,
            showFeatureQueryResults
        ) {
            padding = Insets(0, 250, 0, 250)
            hgrow = Priority.ALWAYS
            maxWidth = Double.MaxValue
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
        val showAnalysisTimes = new MenuItem("Show Analysis Times...") {
            onAction = handle { analysisTimesStage.show() }
        }
        val showQueryResultViz = new MenuItem("Visualize Query Results...") {
            disable <== analysesFinished.not
            onAction = handle { Visualization.display(stage, featureMatrix).show() }
        }
        val showProjectStatistics = new MenuItem("Project Statistics...") {
            // IMPROVE Move it to a "permanent stage" and make the project statistics observable to make it possible to react on changes and to get proper JavaFX behavior

            // Container to store the stages; currently, we have to create the stages lazily
            // because the statistics are not yet observable.
            val pieChartStages: mutable.Map[String, Stage] = mutable.Map.empty
            def createPieChartStage(statistic: String): Stage = {
                val pieChartStage = new Stage {
                    title = statistic
                    scene = new Scene(950, 600) {
                        val pieChartData = ObservableBuffer.empty[javafx.scene.chart.PieChart.Data]
                        val pieChart = new PieChart {
                            data = pieChartData
                            //title = statistic
                            legendVisible = true
                        }

                        featureMatrix foreach { pf ⇒
                            val statistics = pf.projectConfiguration.statistics.get(statistic)
                            pieChartData.add(PieChart.Data(pf.id.value, statistics.get))
                        }

                        val legendButton = new CheckBox("Show Legend (If Place is Sufficient)")
                        pieChart.legendVisible <== legendButton.selected
                        val bp = new BorderPane {
                            center = pieChart
                            top = legendButton
                        }
                        root = bp
                    }
                    initOwner(stage)
                }

                pieChartStage
            }

            disable <== analysesFinished.not
            onAction = handle {
                // all project's have the same statistics, hence, it is sufficient to
                // collect those of the first project
                val someProjectConfiguration = featureMatrix.get(0).projectConfiguration
                val projectStatisticsList = someProjectConfiguration.statistics.keySet

                val statisticDialog =
                    new ChoiceDialog(projectStatisticsList.head, projectStatisticsList) {
                        initOwner(stage)
                        title = "Project Statistics"
                        headerText = "Choose the project statistic to visualize."
                        contentText = "Project Statistic:"
                    }

                statisticDialog.showAndWait() foreach { choice ⇒
                    pieChartStages.getOrElseUpdate(choice, createPieChartStage(choice)).show()
                }
            }
        }

        val fileExport = new MenuItem("Export As...") {
            disable <== analysesFinished.not
            accelerator = KeyCombination.keyCombination("Ctrl +Alt +S")
            onAction = handle {
                val fileChooser = new FileChooser {
                    title = "Open Class File"
                    extensionFilters ++= Seq(
                        new ExtensionFilter("Comma Separated Values", "*.csv")
                    )
                }
                val selectedFile = fileChooser.showSaveDialog(stage)
                if (selectedFile != null) {
                    exportStatistics(selectedFile)
                }
            }
        }

        val computeProjectsForCorpus = new MenuItem("Compute Projects for Corpus...") {
            disable <== analysesFinished.not
            onAction = handle { computeCorpus() }
        }
        List(
            showConfig,
            showQueryResultViz,
            showAnalysisTimes, showProjectStatistics,
            fileExport,
            computeProjectsForCorpus
        )
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
            title = "Hermes - "+parameters.unnamed.head
            root = rootPane

            analyzeCorpus(runAsDaemons = true)
        }
        icons += new Image(getClass.getResource("OPAL-Logo-Small.png").toExternalForm)

        // let's restore the primary window at its last position
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

    val analysisTimesStage = new Stage {
        title = "AnalysisTimes"
        scene = new Scene(800, 600) {
            root = new StackPane {

                val xAxis = new CategoryAxis() {
                    label = "Feature Queries"
                    tickLabelsVisible = false
                }
                corpusAnalysisTime.onChange { (_, _, newValue) ⇒
                    xAxis.label = "Feature Queries ∑"+Nanoseconds(newValue.longValue).toSeconds
                }
                val yAxis = new NumberAxis()

                val data = new ObservableBuffer[javafx.scene.chart.XYChart.Series[String, Number]]()
                for (featureQuery ← featureQueries) {
                    val series = new XYChart.Series[String, Number] {
                        name = featureQuery.id
                    }
                    data.add(series)
                    featureQuery.accumulatedAnalysisTime.onChange { (_, _, newValue) ⇒
                        if (newValue.intValue != 0)
                            series.data = Seq(XYChart.Data[String, Number](featureQuery.id, newValue))
                    }
                }
                val barChart = BarChart(xAxis, yAxis)
                barChart.animated = false
                barChart.legendVisible = true
                barChart.title = "Feature Execution Times"
                barChart.data = data
                barChart.barGap = 1

                children = barChart
            }
        }
        initOwner(stage)
    }
}
