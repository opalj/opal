/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package bugpicker

import javafx.scene.control.SeparatorMenuItem
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.control.SplitPane
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView
import scalafx.stage.Screen
import scalafx.stage.Stage
import scalafx.stage.WindowEvent
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Orientation
import scalafx.scene.input.KeyCombination
import scalafx.scene.input.KeyCodeCombination
import scalafx.scene.Node
import org.opalj.br.analyses.Project
import java.net.URL
import java.io.File
import scalafx.concurrent.Task
import scalafx.concurrent.Service
import javafx.concurrent.{ Service ⇒ jService, Task ⇒ jTask }
import scalafx.application.Platform
import org.opalj.ai.debug.XHTML
import scalafx.scene.layout.BorderPane
import scalafx.concurrent.WorkerStateEvent
import scalafx.stage.StageStyle
import scalafx.event.EventHandler
import scalafx.stage.Modality
import scalafx.scene.input.KeyEvent
import scala.io.Source
import scalafx.scene.input.KeyCode
import javafx.event.EventHandler
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.{ EventType ⇒ ET }
import scalafx.beans.property.DoubleProperty
import scalafx.scene.control.ProgressBar
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox
import java.util.prefs.Preferences
import org.opalj.bugpicker.dialogs.AboutDialog
import org.opalj.bugpicker.dialogs.DialogStage
import org.opalj.bugpicker.dialogs.ProjectInfoDialog
import org.opalj.bugpicker.dialogs.LoadProjectDialog
import org.opalj.bugpicker.dialogs.HelpBrowser
import org.opalj.bugpicker.dialogs.LoadedFiles
import org.opalj.bugpicker.analysis.BugPickerAnalysis
import org.opalj.bugpicker.analysis.AnalysisParameters
import org.opalj.bugpicker.dialogs.AnalysisParametersDialog
import javafx.application.Application
import scalafx.geometry.Rectangle2D

class BugPicker extends Application {

    var project: Project[URL] = null
    var sources: Seq[File] = Seq.empty

    override def start(jStage: javafx.stage.Stage) {
        val stage: Stage = jStage

        val sourceView: WebView = new WebView {
            contextMenuEnabled = false
        }
        val byteView: WebView = new WebView {
            contextMenuEnabled = false
        }
        val reportView: WebView = new WebView {
            contextMenuEnabled = false
            engine.loadContent(Messages.APP_STARTED)
        }
        val tabPane: TabPane = new TabPane {
            this += new Tab {
                text = "Source code"
                content = sourceView
                closable = false
            }
            this += new Tab {
                text = "Bytecode"
                content = byteView
                closable = false
            }
        }

        def screenForStage(stage: Stage): Screen =
            Screen.screensForRectangle(stage.x(), stage.y(), stage.width(), stage.height()).head

        def maximizeOnCurrentScreen(stage: Stage) {
            val currentScreen = screenForStage(stage)
            maximizeOnScreen(stage, currentScreen)
        }

        def maximizeOnScreen(stage: Stage, screen: Screen) {
            val screenDimensions = screen.getVisualBounds()
            stage.x = screenDimensions.minX
            stage.y = screenDimensions.minY
            stage.width = screenDimensions.width
            stage.height = screenDimensions.height
        }

        def showURL(url: String): Unit = getHostServices.showDocument(url)

        val aboutDialog = new AboutDialog(stage, showURL)

        def loadProjectAction(): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
            val preferences = BugPicker.loadFilesFromPreferences()
            val dia = new LoadProjectDialog(preferences)
            val results = dia.show(stage)
            if (results.isDefined && !results.get.projectFiles.isEmpty) {
                BugPicker.storeFilesToPreferences(results.get)
                sourceView.engine.loadContent("")
                byteView.engine.loadContent("")
                reportView.engine.loadContent(Messages.LOADING_STARTED)
                Service {
                    Task[Unit] {
                        val projectAndSources = ProjectHelper.setupProject(results.get, stage)
                        project = projectAndSources._1
                        sources = projectAndSources._2
                        Platform.runLater {
                            tabPane.tabs(0).disable = sources.isEmpty
                            if (sources.isEmpty) tabPane.selectionModel().select(1)
                            reportView.engine.loadContent(Messages.LOADING_FINISHED)
                        }
                    }
                }.start
            } else if (results.isDefined && results.get.projectFiles.isEmpty) {
                DialogStage.showMessage("Error", "You have not specified any classes to be analyzed!", stage)
            }
        }

        def createMenuBar(): MenuBar = {
            new MenuBar {
                menus = Seq(
                    new Menu {
                        text = "_File"
                        mnemonicParsing = true
                        items = Seq(
                            new MenuItem {
                                text = "L_oad"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+O")
                                onAction = loadProjectAction()
                            },
                            new MenuItem {
                                text = "_Project info"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+I")
                                onAction = { e: ActionEvent ⇒ ProjectInfoDialog.show(stage, project, sources) }
                            },
                            new SeparatorMenuItem,
                            new MenuItem {
                                text = "_Quit"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+Q")
                                onAction = { e: ActionEvent ⇒ stage.close }
                            }
                        )
                    },
                    new Menu {
                        text = "_Analysis"
                        mnemonicParsing = true
                        items = Seq(
                            new MenuItem {
                                text = "_Run"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+R")
                                onAction = { e: ActionEvent ⇒
                                    val parameters = BugPicker.loadParametersFromPreferences()
                                    AnalysisRunner.runAnalysis(stage, project, sources, parameters,
                                        sourceView, byteView, reportView, tabPane)
                                }
                            },
                            new MenuItem {
                                text = "_Preferences"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+P")
                                onAction = { e: ActionEvent ⇒
                                    val parameters = BugPicker.loadParametersFromPreferences
                                    val dialog = new AnalysisParametersDialog(stage)
                                    val newParameters = dialog.show(parameters)
                                    if (newParameters.isDefined) {
                                        BugPicker.storeParametersToPreferences(newParameters.get)
                                    }
                                }
                            }
                        )
                    },
                    new Menu {
                        text = "_Help"
                        mnemonicParsing = true
                        items = Seq(
                            new MenuItem {
                                text = "_Browse Help"
                                mnemonicParsing = true
                                accelerator = new KeyCodeCombination(KeyCode.F1)
                                onAction = { e: ActionEvent ⇒ HelpBrowser.show() }
                            },
                            new MenuItem {
                                text = "_About"
                                mnemonicParsing = true
                                onAction = { e: ActionEvent ⇒ aboutDialog.showAndWait() }
                            }
                        )
                    })
            }
        }

        stage.title = "BugPicker"

        stage.scene = new Scene {
            val theScene = this
            root = new VBox {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS
                content = Seq(
                    createMenuBar(),
                    new SplitPane {
                        orientation = Orientation.VERTICAL
                        vgrow = Priority.ALWAYS
                        hgrow = Priority.ALWAYS
                        dividerPositions = 0.4

                        items ++= Seq(reportView, tabPane)
                    }
                )
            }

            stylesheets += BugPicker.defaultAppCSSURL
        }

        stage.handleEvent(WindowEvent.WindowShown) { e: WindowEvent ⇒
            val storedSize = BugPicker.loadWindowSizeFromPreferences()
            if (storedSize.isDefined) {
                val currentScreen = Screen.screensForRectangle(storedSize.get)(0)
                val currentScreenSize = currentScreen.bounds
                if (currentScreenSize.contains(storedSize.get)) {
                    stage.width = storedSize.get.width
                    stage.height = storedSize.get.height
                    stage.x = storedSize.get.minX
                    stage.y = storedSize.get.minY
                } else {
                    maximizeOnScreen(stage, currentScreen)
                }
            } else {
                maximizeOnCurrentScreen(stage)
            }
        }

        stage.onCloseRequest = { e: WindowEvent ⇒
            BugPicker.storeWindowSizeInPreferences(stage.width(), stage.height(), stage.x(), stage.y())
        }

        stage.show()
    }
}

object BugPicker {

    final val PREFERENCES_KEY = "/org/opalj/bugpicker"
    final val PREFERENCES = Preferences.userRoot().node(PREFERENCES_KEY)
    final val PREFERENCES_KEY_CLASSES = "classes"
    final val PREFERENCES_KEY_LIBS = "libs"
    final val PREFERENCES_KEY_SOURCES = "sources"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR = "maxEvalFactor"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME = "maxEvalTime"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES = "maxCardinalityOfIntegerRanges"
    final val PREFERENCES_KEY_WINDOW_SIZE = "windowSize"

    final val defaultAppCSSURL = getClass.getResource("/org/opalj/bugpicker/app.css").toExternalForm

    def loadWindowSizeFromPreferences(): Option[Rectangle2D] = {
        val prefValue = PREFERENCES.get(PREFERENCES_KEY_WINDOW_SIZE, "")
        if (prefValue.isEmpty()) return None
        val Array(w, h, x, y) = prefValue.split(";")
        Some(new Rectangle2D(x.toDouble, y.toDouble, w.toDouble, h.toDouble))
    }

    def storeWindowSizeInPreferences(width: Double, height: Double, x: Double, y: Double) {
        val size = s"${width};${height};${x};${y}"
        BugPicker.PREFERENCES.put(BugPicker.PREFERENCES_KEY_WINDOW_SIZE, size)
    }

    def loadParametersFromPreferences(): AnalysisParameters = {
        val maxEvalFactor = PREFERENCES.getDouble(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR,
            BugPickerAnalysis.defaultMaxEvalFactor)
        val maxEvalTime = PREFERENCES.getInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME,
            BugPickerAnalysis.defaultMaxEvalTime)
        val maxCardinalityOfIntegerRanges = PREFERENCES.getInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES,
            BugPickerAnalysis.defaultMaxCardinalityOfIntegerRanges)
        new AnalysisParameters(maxEvalTime, maxEvalFactor, maxCardinalityOfIntegerRanges)
    }

    def storeParametersToPreferences(parameters: AnalysisParameters) {
        PREFERENCES.putInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME,
            parameters.maxEvalTime)
        PREFERENCES.putDouble(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR,
            parameters.maxEvalFactor)
        PREFERENCES.putInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES,
            parameters.maxCardinalityOfIntegerRanges)
    }

    def storeFilesToPreferences(loadedFiles: LoadedFiles) {
        def filesToPref(key: String, files: Seq[File]) =
            PREFERENCES.put(key, files.mkString(File.pathSeparator))

        filesToPref(PREFERENCES_KEY_CLASSES, loadedFiles.projectFiles)
        filesToPref(PREFERENCES_KEY_SOURCES, loadedFiles.projectSources)
        filesToPref(PREFERENCES_KEY_LIBS, loadedFiles.libraries)
    }

    def loadFilesFromPreferences(): Option[LoadedFiles] = {
        def prefAsFiles(key: String): Seq[File] =
            PREFERENCES.get(key, "").split(File.pathSeparator).filterNot(_.isEmpty).map(new File(_))

        if (!PREFERENCES.nodeExists(""))
            return None
        val classes = prefAsFiles(PREFERENCES_KEY_CLASSES)
        val libs = prefAsFiles(PREFERENCES_KEY_LIBS)
        val sources = prefAsFiles(PREFERENCES_KEY_SOURCES)
        Some(LoadedFiles(projectFiles = classes, projectSources = sources, libraries = libs))
    }

    def main(args: Array[String]) {
        Application.launch(classOf[BugPicker], args: _*)
    }
}
