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
package org.opalj.hermes

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import java.io.File
import java.io.PrintWriter
import java.net.URL
import javafx.event.EventHandler
import javafx.scene.control.CheckBoxTreeItem
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State

import netscape.javascript.JSObject
import play.api.libs.json.JsObject
import play.api.libs.json.Json

import scala.io.Source
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.CheckMenuItem
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.control.RadioMenuItem
import scalafx.scene.control.ToggleGroup
import scalafx.scene.control.TreeView
import scalafx.scene.control.cell.CheckBoxTreeCell
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebEngine
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.FileChooser
import scalafx.stage.Screen
import scalafx.stage.Stage

import org.opalj.io.process
import org.opalj.io.processSource

/**
 * Uses a WebView for javascript-based visualizations of feature query results.
 *
 * @author Alexander Appel
 */
object Visualization {

    class SelectionOption(val name: String) {

        private[this] val options: ListBuffer[SelectionOption] = new ListBuffer[SelectionOption]

        def +=(name: String): ListBuffer[SelectionOption] = options += new SelectionOption(name)

        def +=(other: SelectionOption): ListBuffer[SelectionOption] = options += other

        def -=(name: String): ListBuffer[SelectionOption] = options -= find(name).get

        def find(name: String): Option[SelectionOption] = options.find(_.name == name)
    }

    class D3DataProvider(
            val featureMatrix:   ObservableBuffer[ProjectFeatures[URL]],
            val selection:       SelectionOption,
            val optionSelection: mutable.HashMap[String, Boolean]
    ) {

        def getSelectedProjects(
            accCountThreshold:  Int,
            accSingleDisplayed: Double,
            accTotalDisplayed:  Double
        ): JsObject = {
            val projectSelection: SelectionOption = selection.find("projects").get
            val statisticSelection: SelectionOption = selection.find("statistics").get
            val featureSelection: SelectionOption = selection.find("features").get
            val projects = ArrayBuffer.empty[JsObject]
            featureMatrix.filter(p ⇒ projectSelection.find(p.id.value).isDefined).foreach { project ⇒
                // reformat for sorting
                val statistics: Seq[(String, Double)] = project.projectConfiguration.statistics.toSeq
                val features: Seq[(String, Double)] = project.features.map(f ⇒ {
                    (f.value.id, f.value.count.asInstanceOf[Double])
                })
                val selectedValues = (statistics ++ features)
                    .filter(f ⇒ statisticSelection.find(f._1).isDefined
                        || featureSelection.find(f._1).isDefined)
                    .sortBy(_._2).reverse

                val children = ArrayBuffer.empty[JsObject]
                val total: Double = selectedValues.map(_._2).sum

                if (accCountThreshold > 0 && selectedValues.length > accCountThreshold) {
                    var sum: Double = 0d
                    var rest: Double = 0d
                    val restData = ArrayBuffer.empty[JsObject]
                    selectedValues foreach { value ⇒
                        if ((children.length < 2) || (value._2 >= total * accSingleDisplayed)
                            && (sum + value._2 <= total * accTotalDisplayed)) {
                            children += Json.obj(
                                "id" → value._1,
                                "value" → value._2
                            )
                            sum += value._2
                        } else {
                            restData += Json.obj(
                                "id" → value._1.replace('\n', ' '),
                                "value" → value._2
                            )
                        }
                    }
                    rest = total - sum
                    if (rest > 0) {
                        children += Json.obj(
                            "id" → "<rest>",
                            "value" → rest,
                            "metrics" → Json.toJson(restData.toSeq)
                        )
                    }
                } else {
                    selectedValues foreach { value ⇒
                        children += Json.obj(
                            "id" → value._1,
                            "value" → value._2
                        )
                    }
                }

                projects += Json.obj(
                    "id" → project.id.value,
                    "value" → total,
                    "metrics" → Json.toJson(children.toSeq)
                )
            }
            Json.obj(
                "id" → "flare",
                "options" → Json.toJson(optionSelection),
                "children" → Json.toJson(projects.toSeq)
            )
        }

        def getSingleProject(projectName: String): JsObject = {
            val statisticSelection: SelectionOption = selection.find("statistics").get
            val featureSelection: SelectionOption = selection.find("features").get
            val project = featureMatrix.find(_.id.value == projectName).get
            val statistics: Seq[(String, Double)] = project.projectConfiguration.statistics.toSeq
            val features: Seq[(String, Double)] = project.features.map(f ⇒ {
                (f.value.id, f.value.count.asInstanceOf[Double])
            })
            val selectedValues = (statistics ++ features)
                .filter(f ⇒ statisticSelection.find(f._1).isDefined
                    || featureSelection.find(f._1).isDefined)

            val children = ArrayBuffer.empty[JsObject]
            selectedValues foreach { value ⇒
                children += Json.obj(
                    "id" → value._1,
                    "value" → value._2
                )
            }
            Json.obj(
                "id" → projectName,
                "options" → Json.toJson(optionSelection),
                "children" → Json.toJson(children.toSeq)
            )
        }
    }

    private val scripts = Map[String, String](
        "BubbleChart" -> getClass.getResource("d3js/bubbleChart.js").getPath,
        "BubblePieChart" -> getClass.getResource("d3js/bubblePieChart.js").getPath
    )

    private class OptionDetails(val description: String, val selected: Boolean)

    private val options = Map[String, OptionDetails](
        "Bubbles" -> new OptionDetails("Show Bubble Labels", true),
        "Pies" -> new OptionDetails("Show Pie Labels", true)
    )

    def display(
        mainStage:     Stage,
        featureMatrix: ObservableBuffer[ProjectFeatures[URL]]
    ): Stage = new Stage() { stage ⇒
        title = "Query Result Visualization"
        private val screenBounds = Screen.primary.visualBounds
        private val scaling = 0.67
        scene = new Scene(screenBounds.width * scaling, screenBounds.height * scaling) {
            root = new VBox {
                // setup WebView
                val webView: WebView = new WebView { contextMenuEnabled = false }
                val webEngine: WebEngine = webView.engine
                val stackRight = new StackPane
                stackRight.children.add(webView)

                // setup bridge class object
                val rootSelection: SelectionOption = new SelectionOption("root")
                val labelOptions = new mutable.HashMap[String, Boolean]
                val dataProvider = new D3DataProvider(featureMatrix, rootSelection, labelOptions)

                // register bridge before loading a script
                webEngine.getLoadWorker.stateProperty.addListener(
                    new ChangeListener[State]() {
                        def changed(
                            obs:      ObservableValue[_ <: State],
                            oldState: State, newState: State
                        ): Unit = {
                            if (newState == State.SUCCEEDED) {
                                val bridge = webEngine.executeScript("window").asInstanceOf[JSObject]
                                bridge.setMember("provider", dataProvider)
                                val script = loadScript(getClass.getResource("d3js/bubbleChart.js").getPath)
                                webEngine.executeScript(script)
                                webEngine.executeScript("init();")
                                webEngine.executeScript("display();")
                            }
                        }
                    }
                )
                // load base canvas
                webEngine.load(getClass.getResource("d3js/canvas.html").toExternalForm)

                // build tree items for filter options
                val rootQueries = new CheckBoxTreeItem[String]("root") { setExpanded(true) }
                val filterItems = new CheckBoxTreeItem[String]("Filter") { setExpanded(true) }
                rootQueries.getChildren.add(filterItems)

                // statistics
                val project: ProjectFeatures[URL] = featureMatrix.get(0)
                val statisticSelection: SelectionOption = new SelectionOption("statistics")
                rootSelection += statisticSelection
                val projectStatistic = new CheckBoxTreeItem[String]("Statistics")
                project.projectConfiguration.statistics foreach { statistic ⇒
                    projectStatistic.getChildren.add(
                        factoryCheckBoxTreeItem(statistic._1, statisticSelection)
                    )
                }
                filterItems.getChildren.add(projectStatistic)

                // features
                val projectFeature = new CheckBoxTreeItem[String]("Features")
                val featureSelection: SelectionOption = new SelectionOption("features")
                rootSelection += featureSelection
                project.featureGroups foreach {
                    case (group, features) ⇒
                        val featureGroup = new CheckBoxTreeItem[String](group.id)
                        features foreach { feature ⇒
                            featureGroup.getChildren.add(
                                factoryCheckBoxTreeItem(feature.value.id, featureSelection)
                            )
                        }
                        projectFeature.getChildren.add(featureGroup)
                }
                filterItems.getChildren.add(projectFeature)

                val rootProjects = new CheckBoxTreeItem[String]("root") {
                    setExpanded(true)
                }

                // project names
                val projectSelection: SelectionOption = new SelectionOption("projects")
                rootSelection += projectSelection
                val projectItems = new CheckBoxTreeItem[String]("Projects") {
                    setExpanded(true)
                }
                featureMatrix foreach { project ⇒
                    projectItems.getChildren.add(
                        factoryCheckBoxTreeItem(project.id.value, projectSelection)
                    )
                }
                rootProjects.getChildren.add(projectItems)

                // setup tree view for projects
                val treeViewProjects: TreeView[String] = new TreeView[String]
                treeViewProjects.root = rootProjects
                treeViewProjects.showRoot = false
                treeViewProjects.cellFactory = CheckBoxTreeCell.forTreeView[String]
                // set default selection after registering children
                projectItems.setSelected(true)

                // setup tree view for queries
                val treeViewQueries: TreeView[String] = new TreeView[String]
                treeViewQueries.root = rootQueries
                treeViewQueries.showRoot = false
                treeViewQueries.cellFactory = CheckBoxTreeCell.forTreeView[String]
                // set default selection after registering children
                projectStatistic.setSelected(true)

                // apply button
                val applyButton = new Button("Apply Filter") {
                    maxWidth = Double.MaxValue
                    onAction = handle { webEngine.executeScript("display();") }
                }

                // setup menu bar
                val menuFile = new Menu("File")
                val menuOptions = new Menu("Options")
                val menuView = new Menu("View")

                // svg export
                val svgExport = new MenuItem("Export as SVG") {
                    onAction = handle {
                        val fileChooser = new FileChooser {
                            title = "Save as..."
                            extensionFilters ++= Seq(
                                new ExtensionFilter("Scalable Vector Graphics", "*.svg")
                            )
                        }
                        val selectedFile = fileChooser.showSaveDialog(mainStage)
                        if (selectedFile != null) {
                            val filename = selectedFile.getName
                            val extension = filename.substring(
                                filename.lastIndexOf("."),
                                filename.length()
                            )
                            if (extension.equals(".svg")) {
                                exportSVG(
                                    selectedFile,
                                    webEngine.executeScript("exportSVG();").asInstanceOf[String]
                                )
                            }
                        }
                    }
                }
                menuFile.items.addAll(svgExport)

                // label options
                menuOptions.items = options.map { item ⇒
                    val (name, options) = item
                    new CheckMenuItem(options.description) {
                        onAction = handle {
                            labelOptions += (name → selected.value)
                            webEngine.executeScript("display();")
                        }
                        if (options.selected) {
                            selected = options.selected
                            // manually add to labelOptions if default
                            labelOptions += (name → selected.value)
                        }
                    }
                }(collection.breakOut): List[CheckMenuItem]

                // view options
                val viewToggle = new ToggleGroup
                menuView.items = scripts.map { item ⇒
                    val (name, path) = item
                    new RadioMenuItem(name) {
                        toggleGroup = viewToggle
                        onAction = handle {
                            webEngine.executeScript(loadScript(path))
                            // re-init on view change
                            webEngine.executeScript("init();")
                            webEngine.executeScript("display();")
                            title = stage.title.value+" - "+text.value
                        }
                    }
                }(collection.breakOut): List[RadioMenuItem]
                viewToggle.toggles.get(0).setSelected(true)

                val menuBar = new MenuBar { menus.addAll(menuFile, menuOptions, menuView) }

                // build GUI
                val vBoxLeft = new VBox
                VBox.setVgrow(treeViewQueries, Priority.Always)
                vBoxLeft.children.addAll(applyButton, treeViewProjects, treeViewQueries)

                val mainContent = new HBox
                HBox.setHgrow(stackRight, Priority.Always)
                mainContent.children.addAll(vBoxLeft, stackRight)

                VBox.setVgrow(mainContent, Priority.Always)
                children.addAll(menuBar, mainContent)
            }
        }
        initOwner(mainStage)
    }

    private def exportSVG(file: File, svg: String): Unit = {
        process(new PrintWriter(file)) { _.write(svg) }
    }

    private def loadScript(file: String): String = {
        processSource(Source.fromFile(file)) { s ⇒ s.mkString("\n") }
    }

    private def factoryCheckBoxTreeItem(
        value:           String,
        selectionParent: SelectionOption
    ): CheckBoxTreeItem[String] = {
        new CheckBoxTreeItem[String](value) {
            addEventHandler(
                CheckBoxTreeItem.checkBoxSelectionChangedEvent[String],
                new EventHandler[CheckBoxTreeItem.TreeModificationEvent[String]] {
                    // gets called if a checkbox is updated
                    override def handle(event: CheckBoxTreeItem.TreeModificationEvent[String]): Unit = {
                        if (event.getTreeItem.isSelected) {
                            selectionParent += value
                        } else {
                            selectionParent -= value
                        }
                    }
                }
            )
        }
    }
}
