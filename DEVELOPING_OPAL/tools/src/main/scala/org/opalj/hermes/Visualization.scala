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

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.net.URL
import javafx.event.EventHandler
import javafx.scene.control.CheckBoxTreeItem
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State

import netscape.javascript.JSObject

import scala.collection.mutable
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

/**
 * Implements a WebView for javascript-based visualizations of feature query results
 *
 * @author Alexander Appel
 */
object Visualization {

    private val scripts = new collection.mutable.HashMap[String, String] {
        put("BubbleChart", getClass.getResource("d3js/bubbleChart.js").getPath)
        put("BubblePieChart", getClass.getResource("d3js/bubblePieChart.js").getPath)
    }

    private class OptionDetails(val description: String, val selected: Boolean)

    private val options = new collection.mutable.HashMap[String, OptionDetails] {
        put("Bubbles", new OptionDetails("Show Bubble Labels", true))
        put("Pies", new OptionDetails("Show Pie Labels", true))
    }

    def display(
        mainStage:     Stage,
        featureMatrix: ObservableBuffer[ProjectFeatures[URL]]
    ): Stage = new Stage() {
        val stageTitleBase = "Query Result Visualization"
        title = stageTitleBase
        val screenBounds = Screen.primary.visualBounds
        val scaling = 0.67
        scene = new Scene(screenBounds.width * scaling, screenBounds.height * scaling) {
            root = new VBox {
                // setup WebView
                val webView: WebView = new WebView {
                    contextMenuEnabled = false
                }
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
                val rootQueries = new CheckBoxTreeItem[String]("root") {
                    setExpanded(true)
                }
                val filterItems = new CheckBoxTreeItem[String]("Filter") {
                    setExpanded(true)
                }
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
                val applyButton = new Button("Show Visualization") {
                    maxWidth = Double.MaxValue
                    onAction = handle {
                        webEngine.executeScript("display();")
                    }
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
                menuOptions.items = options.map(item ⇒ {
                    new CheckMenuItem(item._2.description) {
                        onAction = handle {
                            labelOptions += (item._1 → selected.value)
                            webEngine.executeScript("display();")
                        }
                        if (item._2.selected) {
                            selected = item._2.selected
                            // manually add to labelOptions if default
                            labelOptions += (item._1 → selected.value)
                        }
                    }
                })(collection.breakOut): List[CheckMenuItem]

                // view options
                val viewToggle = new ToggleGroup
                menuView.items = scripts.map(item ⇒ {
                    new RadioMenuItem(item._1) {
                        toggleGroup = viewToggle
                        onAction = handle {
                            webEngine.executeScript(loadScript(item._2))
                            // re-init on view change
                            webEngine.executeScript("init();")
                            webEngine.executeScript("display();")
                            title = stageTitleBase+" - "+text.value
                        }
                    }
                })(collection.breakOut): List[RadioMenuItem]
                viewToggle.toggles.get(0).setSelected(true)

                val menuBar = new MenuBar {
                    menus.addAll(menuFile, menuOptions, menuView)
                }

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
        new PrintWriter(file) { write(svg); close() }
    }

    private def loadScript(file: String): String = {
        val br: BufferedReader = new BufferedReader(new FileReader(file))
        val str = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
        br.close()
        str
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
