/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package ui

import java.io.{FileWriter, BufferedWriter, File}
import java.net.URL
import java.util.Date
import java.util.prefs.Preferences
import scala.collection.JavaConverters._
import scala.xml.Node
import scala.xml.dtd.DocType
import scala.xml.dtd.IntDef
import scala.xml.dtd.EntityDef
import scala.xml.dtd.ParsedEntityDecl
import scala.xml.dtd.NoExternalID
import org.opalj.br.analyses.Project
import org.opalj.bugpicker.core.analyses.AnalysisParameters
import org.opalj.bugpicker.ui.dialogs.AboutDialog
import org.opalj.bugpicker.ui.dialogs.DialogStage
import org.opalj.bugpicker.ui.dialogs.DiffView
import org.opalj.bugpicker.ui.dialogs.HelpBrowser
import org.opalj.bugpicker.ui.dialogs.LoadProjectDialog
import org.opalj.bugpicker.ui.dialogs.LoadedFiles
import org.opalj.bugpicker.ui.dialogs.ProjectInfoDialog
import org.opalj.bugpicker.ui.dialogs.StoredAnalysis
import org.opalj.log.OPALLogger
import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.control.ScrollBar
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.Add
import scalafx.collections.ObservableBuffer.Change
import scalafx.collections.ObservableBuffer.Reorder
import scalafx.concurrent.Service
import scalafx.concurrent.Task
import scalafx.concurrent.Task.sfxTask2jfx
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Orientation
import scalafx.geometry.Pos
import scalafx.geometry.Rectangle2D
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.control.TabPane.sfxTabPane2jfx
import scalafx.scene.control.TableCell.sfxTableCell2jfx
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.image.ImageView
import scalafx.scene.input._
import scalafx.scene.input.KeyCode.sfxEnum2jfx
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import scalafx.scene.text.Text
import scalafx.scene.text.Text.sfxText2jfx
import scalafx.scene.web.WebView
import scalafx.scene.web.WebView.sfxWebView2jfx
import scalafx.stage.FileChooser
import scalafx.stage.Screen
import scalafx.stage.Screen.sfxScreen2jfx
import scalafx.stage.Stage
import scalafx.stage.WindowEvent
import org.opalj.log.GlobalLogContext
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory
import org.opalj.bugpicker.ui.dialogs.AnalysisParametersDialog
import org.opalj.bugpicker.core.analyses.AnalysisParameters
import com.typesafe.config.Config
import scala.util.control.ControlThrowable
import org.opalj.issues.Issue

/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author David Becker
 * @author Tobias Becker
 */
class BugPicker extends Application {

    var project: Project[URL] = null
    var sources: Seq[File] = Seq.empty
    var recentProjects: Seq[LoadedFiles] = BugPicker.loadRecentProjectsFromPreferences().getOrElse(Seq.empty)
    var recentAnalyses: Seq[StoredAnalysis] = BugPicker.loadRecentlyUsedAnalyses
    var currentAnalysis: Iterable[Node] = null

    override def start(jStage: javafx.stage.Stage): Unit = {
        val stage: Stage = jStage

        val sourceView: WebView = new WebView {
            contextMenuEnabled = false
        }
        val byteView: WebView = new WebView {
            contextMenuEnabled = false
        }

        val reportView: WebView = new WebView {
            val saveHTMLItem = new MenuItem("save as HTML") {
                onAction = { e: ActionEvent ⇒
                    Platform.runLater {
                        val name = recentProjects.head.projectName+"_report"
                        val fileExt = ".html"
                        val fsd = new FileChooser {
                            title = "Save Report As HTML"
                            extensionFilters ++= Seq(
                                new FileChooser.ExtensionFilter("Bugpicker HTMLReport", fileExt),
                                new FileChooser.ExtensionFilter("All Files", "*.*")
                            )
                            initialDirectory = BugPicker.loadLastDirectoryFromPreferences()
                            initialFileName = name + fileExt
                        }
                        val file = fsd.showSaveDialog(stage)
                        if (file != null) {
                            val html = engine.executeScript("document.documentElement.outerHTML").toString
                            val writer = new BufferedWriter(new FileWriter(file))
                            writer.write(html)
                            writer.flush()
                            writer.close()
                        }
                    }
                }
            }

            val contextMenu = new ContextMenu(saveHTMLItem)
            onMouseClicked = { e: MouseEvent ⇒
                if ((e.getButton eq MouseButton.sfxEnum2jfx(MouseButton.Secondary))
                    && (currentAnalysis ne null)) {
                    contextMenu.show(this, e.getScreenX(), e.getScreenY())
                } else {
                    contextMenu.hide()
                }
            }

            contextMenuEnabled = false
            engine.loadContent(Messages.APP_STARTED)
        }

        def createLogMessagesTableView(
            messages: ObservableBuffer[BugPickerLogMessage]
        ): TableView[BugPickerLogMessage] = {
            val timestampColumn = new TableColumn[BugPickerLogMessage, String] {
                text = "Timestamp"
                maxWidth = 100
                resizable = false
                cellValueFactory = { _.value.timestamp }
            }
            val levelColumn = new TableColumn[BugPickerLogMessage, String] {
                text = "Level"
                maxWidth = 75
                resizable = false
                cellValueFactory = { _.value.level }
                cellFactory = { (_: Object) ⇒
                    new TableCell[BugPickerLogMessage, String] {
                        item.onChange { (_, _, newV) ⇒
                            newV match {
                                case "warn"  ⇒ textFill = Color.Blue
                                case "error" ⇒ textFill = Color.Red
                                case _       ⇒ textFill = Color.Black
                            }
                            text = newV
                        }
                    }
                }
            }
            val categoryColumn = new TableColumn[BugPickerLogMessage, String] {
                text = "Category"
                maxWidth = 150
                resizable = false
                cellValueFactory = { _.value.category }
            }
            val messageColumn = new TableColumn[BugPickerLogMessage, String] {
                text = "Message"
                resizable = false
                cellValueFactory = { _.value.message }
                cellFactory = { (_: Object) ⇒
                    val tc = new TableCell[BugPickerLogMessage, String]
                    val text = new Text()
                    text.wrappingWidthProperty().bind(tc.widthProperty().subtract(20))
                    text.textProperty().bind(tc.itemProperty())
                    tc.setGraphic(text)
                    tc.wrapText = true
                    tc
                }
            }
            val logMessagesTableView = new TableView[BugPickerLogMessage](messages) {
                columns ++= Seq(
                    timestampColumn,
                    levelColumn,
                    categoryColumn,
                    messageColumn
                )
                editable = false
                placeholder = Label("")
            }
            timestampColumn.prefWidthProperty().bind(
                logMessagesTableView.widthProperty().multiply(0.17)
            )
            levelColumn.prefWidthProperty().bind(
                logMessagesTableView.widthProperty().multiply(0.13)
            )
            categoryColumn.prefWidthProperty().bind(
                logMessagesTableView.widthProperty().multiply(0.26)
            )
            messageColumn.prefWidthProperty().bind(
                logMessagesTableView.widthProperty().subtract(
                    timestampColumn.widthProperty()
                ).subtract(
                        levelColumn.widthProperty()
                    ).subtract(
                            categoryColumn.widthProperty()
                        )
            )

            logMessagesTableView
        }

        def extractScrollBar(table: TableView[BugPickerLogMessage]): Option[ScrollBar] = {

            table.lookupAll(".scroll-bar:vertical").asScala.foreach { node ⇒
                node match {
                    case s: ScrollBar ⇒ {
                        if (s.getOrientation().equals(javafx.geometry.Orientation.VERTICAL))
                            return Some(s)
                    }
                    case _ ⇒
                }
            }
            None
        }

        def logMessagesOnChangeBehaviour(
            sortOrder: ObservableBuffer[javafx.scene.control.TableColumn[BugPickerLogMessage, _]],
            tv:        TableView[BugPickerLogMessage], scrollBar: Option[ScrollBar]
        ): (ObservableBuffer[BugPickerLogMessage], Seq[Change[_]]) ⇒ Unit = {
            (_, changes) ⇒
                {
                    for (change ← changes)
                        change match {
                            case _: Reorder[_] ⇒ {
                                // sortOrder changed, store it
                                sortOrder.clear()
                                sortOrder ++= tv.sortOrder
                            }
                            case Add(pos, _) ⇒ {
                                if (!sortOrder.isEmpty) {
                                    // table was sorted before Add, retain sorting
                                    tv.sortOrder.clear()
                                    tv.sortOrder ++= sortOrder
                                } else {
                                    // table isn't sorted, scroll to new element
                                    if (scrollBar.isDefined && scrollBar.get.isVisible())
                                        tv.scrollTo(pos)
                                }
                            }
                            case _ ⇒ {
                                if (!sortOrder.isEmpty) {
                                    tv.sortOrder.clear()
                                    tv.sortOrder ++= sortOrder
                                }
                            }
                        }
                }
        }

        val bugpickerLogMessages = ObservableBuffer[BugPickerLogMessage]()
        val projectLogMessages = ObservableBuffer[BugPickerLogMessage]()
        val bugpickerLogView: TableView[BugPickerLogMessage] =
            createLogMessagesTableView(bugpickerLogMessages)
        val projectLogView: TableView[BugPickerLogMessage] =
            createLogMessagesTableView(projectLogMessages)
        val bugpickerLogSortOrder = ObservableBuffer(bugpickerLogView.sortOrder)
        val projectLogSortOrder = ObservableBuffer(projectLogView.sortOrder)

        val tabPane: TabPane = new TabPane {
            this += new Tab {
                text = "BugPicker Log"
                content = bugpickerLogView
                closable = false
            }
            this += new Tab {
                text = "Project Log"
                content = projectLogView
                closable = false
            }
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
        val projectExplorerShown: VBox = new VBox {
            vgrow = Priority.Always
            hgrow = Priority.Always
            alignment = Pos.TopRight
            padding = Insets(5, 10, 10, 10)
        }
        val projectExplorerHidden: VBox = new VBox {
            vgrow = Priority.Always
            hgrow = Priority.Always
            alignment = Pos.TopRight
            padding = Insets(5, 7, 0, 4)
        }
        val contentPane = new SplitPane {
            orientation = Orientation.Vertical
            vgrow = Priority.Always
            hgrow = Priority.Always
            dividerPositions = 0.45d

            items ++= Seq(reportView, tabPane)
        }
        val bugPickerPane = new SplitPane {
            orientation = Orientation.Horizontal
            vgrow = Priority.Always
            hgrow = Priority.Always
            dividerPositions = 0.20d

            items ++= Seq(contentPane)
        }
        val restoreProjectExplorerButton = new Button {
            minWidth = 15
            maxWidth = 15
            id = "projectExplorer"
            tooltip = new Tooltip {
                text = "Restore"
            }
            graphic = new ImageView {
                image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/restore_view.gif")
            }
        }
        val minimizeProjectExplorerButton = new Button {
            id = "projectExplorer"
            tooltip = new Tooltip {
                text = "Minimize"
            }
            graphic = new ImageView {
                image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/minimize_view.gif")
            }
            onAction = { e: ActionEvent ⇒
                bugPickerPane.items.remove(0)
                bugPickerPane.items.add(0, projectExplorerHidden)
                bugPickerPane.setDividerPositions(0.00d)
                bugPickerPane.layout
            }
        }
        restoreProjectExplorerButton.onAction = { e: ActionEvent ⇒
            bugPickerPane.items.remove(0)
            bugPickerPane.items.add(0, projectExplorerShown)
            bugPickerPane.setDividerPositions(0.20d)
            bugPickerPane.layout
        }
        val projectExplorerTreeView: TreeView[ProjectExplorerData] = new TreeView[ProjectExplorerData] {
            vgrow = Priority.Always
            hgrow = Priority.Always
            minWidth = 150
            minHeight = 200
            showRoot = true
            visible = false
        }
        val projectExplorer = new ProjectExplorer(projectExplorerTreeView, stage)

        projectExplorerShown.children = Seq(
            minimizeProjectExplorerButton,
            projectExplorerTreeView
        )
        projectExplorerHidden.children = Seq(
            restoreProjectExplorerButton
        )

        OPALLogger.updateLogger(GlobalLogContext, new BugPickerOPALLogger(bugpickerLogMessages))

        def screenForStage(stage: Stage): Screen =
            Screen.screensForRectangle(stage.x(), stage.y(), stage.width(), stage.height()).head

        def maximizeOnCurrentScreen(stage: Stage): Unit = {
            val currentScreen = screenForStage(stage)
            maximizeOnScreen(stage, currentScreen)
        }

        def maximizeOnScreen(stage: Stage, screen: Screen): Unit = {
            val screenDimensions = screen.getVisualBounds()
            stage.x = screenDimensions.minX
            stage.y = screenDimensions.minY
            stage.width = screenDimensions.width
            stage.height = screenDimensions.height
        }

        def showURL(url: String): Unit = getHostServices.showDocument(url)

        val aboutDialog = new AboutDialog(stage, showURL)

        lazy val recentProjectsMenu = new Menu {
            text = "_Open Recent"
            mnemonicParsing = true
            items = createRecentProjectsMenu()
            if (items.isEmpty) {
                disable = true
            }
        }

        lazy val storeCurrentAnalysis = new MenuItem {
            text = "_Store Analysis"
            mnemonicParsing = true
            accelerator = KeyCombination("Shortcut+S")
            disable = true
            onAction = { e: ActionEvent ⇒
                Platform.runLater {
                    val name = recentProjects.head.projectName
                    val bpFileSuffix = "*"+BugPicker.BUGPICKER_ANALYSIS_FILE_EXTENSION
                    val fsd = new FileChooser {
                        title = "Save Analysis"
                        extensionFilters ++= Seq(
                            new FileChooser.ExtensionFilter("Bugpicker Analysis", bpFileSuffix),
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                        )
                        initialDirectory = BugPicker.loadLastDirectoryFromPreferences()
                        initialFileName = name + BugPicker.BUGPICKER_ANALYSIS_FILE_EXTENSION
                    }
                    val file = fsd.showSaveDialog(stage)
                    if (file != null) {
                        BugPicker.storeLastDirectoryToPreferences(file.getParentFile)
                        recentAnalyses =
                            BugPicker.storeAnalysis(
                                name,
                                currentAnalysis,
                                file
                            )(
                                recentAnalyses
                            )
                        recentAnalysisToDiffMenu.items = createRecentAnalysisMenu()
                        recentAnalysisToDiffMenu.disable = false
                    }
                }
            }
        }

        lazy val recentAnalysisToDiffMenu = new Menu {
            text = "Recent Diff"
            items = createRecentAnalysisMenu()
            disable = true
        }

        lazy val loadAnalysisToDiff = new MenuItem {
            text = "Load Diff"
            disable = true
            onAction = { e: ActionEvent ⇒
                val fod = new FileChooser {
                    title = "Open Analysis"
                    extensionFilters ++= Seq(
                        new FileChooser.ExtensionFilter("Bugpicker Analyis", "*"+BugPicker.BUGPICKER_ANALYSIS_FILE_EXTENSION),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                    )
                    initialDirectory = BugPicker.loadLastDirectoryFromPreferences()
                }
                val file = fod.showOpenDialog(stage)
                if (file != null) {
                    BugPicker.storeLastDirectoryToPreferences(file.getParentFile)
                    val oldAnalysis = StoredAnalysis.readFromFile(file)
                    if (oldAnalysis.isDefined) {
                        val dv =
                            new DiffView(
                                recentProjects.head.projectName,
                                currentAnalysis,
                                Seq.empty[String], // FIXME analysisParameter are not supported anymore
                                oldAnalysis.get
                            )
                        recentAnalyses = BugPicker.updateRecentlyUsedAnalyses(oldAnalysis.get, recentAnalyses)
                        recentAnalysisToDiffMenu.items = createRecentAnalysisMenu()
                        recentAnalysisToDiffMenu.disable = false
                        dv.show(stage)
                    } else {
                        DialogStage.showMessage("Error", "Not an BugPickerAnalysis-File.", stage)
                    }
                }
            }
        }

        lazy val preferencesMenuItem = new MenuItem {
            disable = true
            text = "_Preferences"
            mnemonicParsing = true
            accelerator = KeyCombination("Shortcut+P")
            onAction = { e: ActionEvent ⇒
                Platform.runLater {
                    val dialog = new AnalysisParametersDialog(stage)
                    val newConfiguration = dialog.show(project.config)
                    if (newConfiguration.isDefined) {
                        project = Project.recreate(project, newConfiguration.get, true)
                        BugPicker.storeConfigToPreferences(project)
                        val preferences = BugPicker.loadFilesFromPreferences
                        if (preferences.isDefined) {
                            BugPicker.storeFilesToPreferences(preferences.get)
                            recentProjects = updateRecentProjects(preferences.get)
                            BugPicker.storeRecentProjectsToPreferences(recentProjects)
                        }
                    }
                }
            }
        }

        def loadProjectAction(): Unit = {
            val preferences = BugPicker.loadFilesFromPreferences()
            val dia = new LoadProjectDialog(preferences, recentProjects)
            val results = dia.show(stage)
            if (results.isDefined && !results.get.projectFiles.isEmpty) {
                BugPicker.storeFilesToPreferences(results.get)
                recentProjects = updateRecentProjects(results.get)
                BugPicker.storeRecentProjectsToPreferences(recentProjects)
                recentProjectsMenu.items = createRecentProjectsMenu()
                sourceView.engine.loadContent("")
                byteView.engine.loadContent("")
                bugpickerLogMessages.clear()
                reportView.engine.loadContent(Messages.LOADING_STARTED)
                projectExplorer.reset(results.get.projectName)
                Service {
                    Task[Unit] {
                        try {
                            val projectAndSources = ProjectHelper.setupProject(results.get, stage, projectLogMessages)
                            project = projectAndSources._1
                            sources = projectAndSources._2
                            BugPicker.storeConfigToPreferences(project)
                            recentProjects = updateRecentProjects(results.get)
                            BugPicker.storeRecentProjectsToPreferences(recentProjects)
                        } catch {
                            case ct: ControlThrowable ⇒ throw ct
                            case t: Throwable ⇒
                                OPALLogger.error(
                                    "creating project",
                                    "failed to initialize project: "+t.getMessage,
                                    t
                                )(GlobalLogContext)
                                throw t
                        }
                        Platform.runLater {
                            tabPane.tabs(2).disable = sources.isEmpty
                            if (!tabPane.selectionModel().isSelected(1)) {
                                tabPane.selectionModel().select(1)
                            }
                            reportView.engine.loadContent(Messages.LOADING_FINISHED)
                            projectExplorer.buildProjectExplorer(project, sources, results.get.projectName)
                            if (!projectExplorerTreeView.isVisible()) {
                                projectExplorerTreeView.visible = true
                                projectExplorerTreeView.visible = true
                                bugPickerPane.items.add(0, projectExplorerShown)
                            }

                            recentProjectsMenu.disable = false
                            preferencesMenuItem.disable = false
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
                        text = "_Project"
                        mnemonicParsing = true
                        items = Seq(
                            new MenuItem {
                                text = "_New Project"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+N")
                                onAction = { e: ActionEvent ⇒
                                    Platform.runLater {
                                        loadProjectAction()
                                    }
                                }
                            },
                            recentProjectsMenu,
                            new MenuItem {
                                text = "Project _Info"
                                mnemonicParsing = true
                                accelerator = KeyCombination("Shortcut+I")
                                onAction = { e: ActionEvent ⇒
                                    Platform.runLater {
                                        ProjectInfoDialog.show(stage, project, sources)
                                    }
                                }
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
                                    Platform.runLater {
                                        storeCurrentAnalysis.disable = true
                                        val issues = ObjectProperty(Iterable.empty[Issue])
                                        issues.onChange((o, p, q) ⇒ {
                                            currentAnalysis = issues().map(_.toXHTML(false))
                                            if (currentAnalysis != null) {
                                                storeCurrentAnalysis.disable = false
                                                loadAnalysisToDiff.disable = false
                                                if (!recentAnalysisToDiffMenu.items.isEmpty)
                                                    recentAnalysisToDiffMenu.disable = false
                                            }
                                        })
                                        AnalysisRunner.runAnalysis(
                                            stage, project, sources,
                                            issues, sourceView, byteView, reportView, tabPane
                                        )

                                        if (!tabPane.selectionModel().isSelected(1)) {
                                            tabPane.selectionModel().select(1)
                                        }
                                    }
                                }
                            },
                            preferencesMenuItem //,
                        //                            new SeparatorMenuItem,
                        //                            storeCurrentAnalysis,
                        //                            loadAnalysisToDiff,
                        //                            recentAnalysisToDiffMenu
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
                    }
                )
            }
        }

        def createRecentAnalysisMenu(): Seq[MenuItem] = {

            if (!recentAnalyses.isEmpty) {
                (recentAnalyses.map { p ⇒
                    new MenuItem {
                        text = p.analysisName+" - "+p.analysisDate
                        mnemonicParsing = true
                        disable = p == currentAnalysis
                        onAction = { e: ActionEvent ⇒
                            Platform.runLater {
                                new DiffView(
                                    recentProjects.head.projectName,
                                    currentAnalysis,
                                    Seq.empty[String], //FIXME currentAnalysisParameters.toStringParameters,
                                    p
                                ).show(stage)
                            }
                        }
                    }
                }) ++ Seq(
                    new MenuItem {
                        text = "Clear Analyses"
                        mnemonicParsing = true
                        id = "clearAnalyses"
                        onAction = { e: ActionEvent ⇒
                            recentAnalyses = Seq.empty
                            BugPicker.deleteAnalyses()
                            // recentAnalysisMenu should now be empty, disable it
                            recentAnalysisToDiffMenu.items = Seq.empty
                            recentAnalysisToDiffMenu.disable = true;
                        }
                    }
                )
            } else {
                Seq.empty
            }
        }

        def createRecentProjectsMenu(): Seq[MenuItem] = {

            if (!recentProjects.isEmpty) {
                var i = 0
                (for (p ← recentProjects) yield {
                    i = i + 1
                    new MenuItem {
                        text = p.projectName flatMap {
                            case '_' ⇒ "__"
                            case c   ⇒ s"$c"
                        }
                        mnemonicParsing = true
                        if (i < 10) {
                            accelerator = KeyCombination(s"Shortcut+$i")
                        }
                        onAction = { e: ActionEvent ⇒
                            Platform.runLater {
                                BugPicker.storeFilesToPreferences(p)
                                loadProjectAction()
                            }
                        }
                    }
                }) ++ Seq(
                    new SeparatorMenuItem,
                    new MenuItem {
                        text = "_Clear Items"
                        mnemonicParsing = true
                        accelerator = KeyCombination("Shortcut+0")
                        onAction = { e: ActionEvent ⇒
                            Platform.runLater {
                                recentProjects = Seq.empty
                                BugPicker.deleteRecentProjectsFromPreferences()
                                recentProjectsMenu.items = Seq.empty
                                recentProjectsMenu.disable = true;
                            }
                        }
                    }
                )
            } else {
                Seq.empty
            }
        }

        def updateRecentProjects(lastProject: LoadedFiles): Seq[LoadedFiles] = {
            if (recentProjects.contains(lastProject)) {
                // lastProject isn't already most recent project, bring it to front
                if (recentProjects.head != lastProject) {
                    lastProject +: recentProjects.filter(_ != lastProject)
                } else {
                    recentProjects
                }
            } else if (recentProjects.exists(_.projectName == lastProject.projectName)) {
                // already existing project got updated, bring it to front
                lastProject +: recentProjects.filter(
                    _.projectName != lastProject.projectName
                )
            } else {
                if (recentProjects.size < BugPicker.MAX_RECENT_PROJECTS) {
                    // lastProject is most recent project, enough space for one more
                    lastProject +: recentProjects
                } else {
                    // lastProject is most recent project, drop least recent project
                    lastProject +: recentProjects.init
                }
            }
        }

        stage.title = "BugPicker"

        stage.scene = new Scene {

            root = new VBox {
                vgrow = Priority.Always
                hgrow = Priority.Always
                children = Seq(
                    createMenuBar(),
                    bugPickerPane
                )
            }

            stylesheets += BugPicker.defaultAppCSSURL
        }

        stage.handleEvent(WindowEvent.WindowShown) { e: WindowEvent ⇒
            val storedSize = BugPicker.loadWindowSizeFromPreferences()
            if (storedSize.isDefined) {
                val screens = Screen.screensForRectangle(storedSize.get)
                if (screens.isEmpty) {
                    maximizeOnCurrentScreen(stage)
                } else {
                    val currentScreen = screens(0)
                    val currentScreenSize = currentScreen.bounds
                    if (currentScreenSize.contains(storedSize.get)) {
                        stage.width = storedSize.get.width
                        stage.height = storedSize.get.height
                        stage.x = storedSize.get.minX
                        stage.y = storedSize.get.minY
                    } else {
                        maximizeOnScreen(stage, currentScreen)
                    }
                }
            } else {
                maximizeOnCurrentScreen(stage)
            }
        }

        stage.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, new EventHandler[javafx.scene.input.MouseEvent]() {
            def handle(e: javafx.scene.input.MouseEvent): Unit = {
                if (e.button == MouseButton.Secondary) {
                    stage.toFront()
                }
            }
        })

        stage.onCloseRequest = { e: WindowEvent ⇒
            BugPicker.storeWindowSizeInPreferences(
                stage.width(), stage.height(),
                stage.x(), stage.y()
            )
        }

        stage.show()

        val bugpickerLogViewScrollBar: Option[ScrollBar] = extractScrollBar(bugpickerLogView)
        val projectLogViewScrollBar: Option[ScrollBar] = extractScrollBar(projectLogView)

        bugpickerLogMessages.onChange {
            logMessagesOnChangeBehaviour(
                bugpickerLogSortOrder, bugpickerLogView, bugpickerLogViewScrollBar
            )
        }
        projectLogMessages.onChange {
            logMessagesOnChangeBehaviour(
                projectLogSortOrder, projectLogView, projectLogViewScrollBar
            )
        }
    }
}

object BugPicker {

    final val PREFERENCES_KEY = "/org/opalj/bugpicker"
    final val PREFERENCES = Preferences.userRoot().node(PREFERENCES_KEY)
    final val PREFERENCES_KEY_CURRENT_PROJECT_NAME = "name"
    final val PREFERENCES_KEY_CURRENT_PROJECT_CLASSES = "classes"
    final val PREFERENCES_KEY_CURRENT_PROJECT_LIBS = "libs"
    final val PREFERENCES_KEY_CURRENT_PROJECT_SOURCES = "sources"
    final val PREFERENCES_KEY_CURRENT_PROJECT_CONFIG = "config"
    final val RECENT_PROJECT = "recentProject_"
    final val PREFERENCES_KEY_RECENT_PROJECT_NAME = "name_"
    final val PREFERENCES_KEY_RECENT_PROJECT_CLASSES = "classes_"
    final val PREFERENCES_KEY_RECENT_PROJECT_LIBS = "libs_"
    final val PREFERENCES_KEY_RECENT_PROJECT_SOURCES = "sources_"
    final val PREFERENCES_KEY_RECENT_PROJECT_CONFIG = "config_"
    final val RECENT_ANALYSIS = "recentAnalysis_"
    final val PREFERENCES_KEY_RECENT_ANALYSIS_NAME = "name"
    final val PREFERENCES_KEY_RECENT_ANALYSIS_DATE = "date"
    final val PREFERENCES_KEY_RECENT_ANALYSIS_FILE = "file"
    final val PREFERENCES_KEY_CONFIG = "config"
    final val PREFERENCES_KEY_WINDOW_SIZE = "windowSize"
    final val PREFERENCES_KEY_LAST_DIRECTORY = "lastDirectory"

    final val defaultAppCSSURL = getClass.getResource("/org/opalj/bugpicker/ui/app.css").toExternalForm

    final val MAX_RECENT_PROJECTS = 9

    final val MAX_RECENT_ANALYSES = 7
    final val BUGPICKER_ANALYSIS_FILE_EXTENSION = ".bpa"

    def storeLastDirectoryToPreferences(file: File) = {
        BugPicker.PREFERENCES.put(BugPicker.PREFERENCES_KEY_LAST_DIRECTORY, file.getAbsolutePath)
    }

    def loadLastDirectoryFromPreferences(): File = {
        new File(BugPicker.PREFERENCES.get(BugPicker.PREFERENCES_KEY_LAST_DIRECTORY, System.getProperty("user.home")))
    }

    def loadWindowSizeFromPreferences(): Option[Rectangle2D] = {
        val prefValue = PREFERENCES.get(PREFERENCES_KEY_WINDOW_SIZE, "")
        if (prefValue.isEmpty()) {
            return None
        }
        val Array(w, h, x, y) = prefValue.split(";")
        Some(new Rectangle2D(x.toDouble, y.toDouble, w.toDouble, h.toDouble))
    }

    def storeWindowSizeInPreferences(width: Double, height: Double, x: Double, y: Double): Unit = {
        val size = s"${width};${height};${x};${y}"
        BugPicker.PREFERENCES.put(BugPicker.PREFERENCES_KEY_WINDOW_SIZE, size)
    }

    def storeConfigToPreferences(project: Project[URL]): Unit = {
        val configString = renderConfig(project.config)
        PREFERENCES.put(PREFERENCES_KEY_CURRENT_PROJECT_CONFIG, configString)
    }

    def storeFilesToPreferences(loadedFiles: LoadedFiles): Unit = {
        def filesToPref(key: String, files: Seq[File]) =
            PREFERENCES.put(key, files.mkString(File.pathSeparator))

        PREFERENCES.put(PREFERENCES_KEY_CURRENT_PROJECT_NAME, loadedFiles.projectName)
        filesToPref(PREFERENCES_KEY_CURRENT_PROJECT_CLASSES, loadedFiles.projectFiles)
        filesToPref(PREFERENCES_KEY_CURRENT_PROJECT_SOURCES, loadedFiles.projectSources)
        filesToPref(PREFERENCES_KEY_CURRENT_PROJECT_LIBS, loadedFiles.libraries)
        if (loadedFiles.config.isDefined) {
            val configString = renderConfig(loadedFiles.config.get)
            PREFERENCES.put(PREFERENCES_KEY_CURRENT_PROJECT_CONFIG, configString)
        }
    }

    def loadFilesFromPreferences(): Option[LoadedFiles] = {
        def prefAsFiles(key: String): Seq[File] =
            PREFERENCES.get(key, "").split(File.pathSeparator).filterNot(_.isEmpty).map(new File(_))

        if (!PREFERENCES.nodeExists("")) {
            return None
        }
        val name = PREFERENCES.get(PREFERENCES_KEY_CURRENT_PROJECT_NAME, "")
        val classes = prefAsFiles(PREFERENCES_KEY_CURRENT_PROJECT_CLASSES)
        val libs = prefAsFiles(PREFERENCES_KEY_CURRENT_PROJECT_LIBS)
        val sources = prefAsFiles(PREFERENCES_KEY_CURRENT_PROJECT_SOURCES)
        val storedConfString = PREFERENCES.get(PREFERENCES_KEY_CURRENT_PROJECT_CONFIG, "")
        val conf = ConfigFactory.parseString(storedConfString)

        Some(LoadedFiles(
            projectName = name,
            projectFiles = classes,
            projectSources = sources,
            libraries = libs,
            config = Some(conf)
        ))
    }

    def renderConfig(config: Config, withComments: Boolean = true): String = {
        val renderingOptions = ConfigRenderOptions.
            defaults().
            setOriginComments(false).
            setComments(withComments).
            setJson(false);
        val bugpickerConf = config.withOnlyPath("org.opalj")
        bugpickerConf.root().render(renderingOptions)
    }

    def storeRecentProjectsToPreferences(recentProjects: Seq[LoadedFiles]): Unit = {
        def filesToPref(key: String, files: Seq[File], node: Preferences) =
            node.put(key, files.mkString(File.pathSeparator))

        var i = 0

        for (loadedFiles ← recentProjects) {
            i = i + 1
            val PREFERENCES_RECENT_PROJECT = PREFERENCES.node(s"$RECENT_PROJECT$i")
            PREFERENCES_RECENT_PROJECT.put(
                s"$PREFERENCES_KEY_RECENT_PROJECT_NAME$i",
                loadedFiles.projectName
            )
            filesToPref(
                s"$PREFERENCES_KEY_RECENT_PROJECT_CLASSES$i",
                loadedFiles.projectFiles,
                PREFERENCES_RECENT_PROJECT
            )
            filesToPref(
                s"$PREFERENCES_KEY_RECENT_PROJECT_SOURCES$i",
                loadedFiles.projectSources,
                PREFERENCES_RECENT_PROJECT
            )
            filesToPref(
                s"$PREFERENCES_KEY_RECENT_PROJECT_LIBS$i",
                loadedFiles.libraries,
                PREFERENCES_RECENT_PROJECT
            )

            val fileConfig = ConfigFactory.load()
            val currentConfig = loadedFiles.config match {
                case Some(config) ⇒ config.withFallback(fileConfig)
                case None         ⇒ ConfigFactory.load()
            }

            PREFERENCES_RECENT_PROJECT.put(
                s"$PREFERENCES_KEY_RECENT_PROJECT_CONFIG$i",
                renderConfig(currentConfig)
            )
        }
    }

    def deleteRecentProjectsFromPreferences(): Unit = {
        for (i ← 1 to PREFERENCES.childrenNames().size) {
            val PREFERENCES_RECENT_PROJECT = PREFERENCES.node(s"$RECENT_PROJECT$i")
            PREFERENCES_RECENT_PROJECT.clear()
            PREFERENCES_RECENT_PROJECT.removeNode()
        }
    }

    def loadRecentProjectsFromPreferences(): Option[Seq[LoadedFiles]] = {
        def prefAsFiles(key: String, node: Preferences): Seq[File] =
            node.get(key, "").split(File.pathSeparator).filterNot(_.isEmpty).map(new File(_))

        if (!PREFERENCES.nodeExists("")) {
            return None
        }
        val result = for (i ← 1 to PREFERENCES.childrenNames().size; if PREFERENCES.nodeExists(s"$RECENT_PROJECT$i")) yield {
            val PREFERENCES_RECENT_PROJECT = PREFERENCES.node(s"$RECENT_PROJECT$i")
            val name = PREFERENCES_RECENT_PROJECT.get(s"$PREFERENCES_KEY_RECENT_PROJECT_NAME$i", "")
            val classes = prefAsFiles(s"$PREFERENCES_KEY_RECENT_PROJECT_CLASSES$i", PREFERENCES_RECENT_PROJECT)
            val libs = prefAsFiles(s"$PREFERENCES_KEY_RECENT_PROJECT_LIBS$i", PREFERENCES_RECENT_PROJECT)
            val sources = prefAsFiles(s"$PREFERENCES_KEY_RECENT_PROJECT_SOURCES$i", PREFERENCES_RECENT_PROJECT)
            val confString = PREFERENCES_RECENT_PROJECT.get(s"$PREFERENCES_KEY_RECENT_PROJECT_CONFIG$i", "")
            val conf = Some(ConfigFactory.parseString(confString))
            LoadedFiles(projectName = name, projectFiles = classes, projectSources = sources, libraries = libs, config = conf)
        }
        Some(result)
    }

    def storeAnalysis(
        name:       String,
        issues:     Iterable[Node],
        file:       File,
        parameters: AnalysisParameters = new AnalysisParameters // FIXME these parameters are not used anymore
    )(
        recentAnalyses: Seq[StoredAnalysis]
    ): Seq[StoredAnalysis] = {

        class PE(name: String, entdef: EntityDef) extends ParsedEntityDecl(name, entdef) {
            override def toString: String = {
                val sb = new StringBuilder()
                return this.buildString(sb).toString
            }
        }
        // 1. create new StoredAnalysis
        val newStoredAnalysis = StoredAnalysis(name, file)

        // 2. create XML-output and save it to file
        val data =
            <analysis name={ newStoredAnalysis.analysisName } date={ newStoredAnalysis.analysisDate.getTime.toString }>
                <parameters>
                    { parameters.toStringParameters.map { x ⇒ <parameter>{ x }</parameter> } }
                </parameters>
                <issues>
                    { issues }
                </issues>
            </analysis>
        scala.xml.XML.save(file.getAbsolutePath, data, "UTF-8", true, DocType("issues", NoExternalID, Seq(new PE("nbsp", IntDef("&#160;")))))

        // 3. update recently used analyses in Prefs
        updateRecentlyUsedAnalyses(newStoredAnalysis, recentAnalyses)
    }

    def updateRecentlyUsedAnalyses(newAnalysis: StoredAnalysis, recentAnalyses: Seq[StoredAnalysis]) = {
        val updatedAnalyses = ((newAnalysis +: recentAnalyses.filter(_ != newAnalysis)).take(BugPicker.MAX_RECENT_ANALYSES))

        var i = 0
        for (recentAnalysis ← updatedAnalyses) {
            i = i + 1
            val PREFERENCES_RECENT_ANALYSIS = PREFERENCES.node(s"$RECENT_ANALYSIS$i")
            PREFERENCES_RECENT_ANALYSIS.put(s"$PREFERENCES_KEY_RECENT_ANALYSIS_NAME", recentAnalysis.analysisName)
            PREFERENCES_RECENT_ANALYSIS.put(s"$PREFERENCES_KEY_RECENT_ANALYSIS_DATE", recentAnalysis.analysisDate.getTime.toString)
            PREFERENCES_RECENT_ANALYSIS.put(s"$PREFERENCES_KEY_RECENT_ANALYSIS_FILE", recentAnalysis.analysisFile.getAbsolutePath)
        }
        updatedAnalyses
    }

    def loadRecentlyUsedAnalyses(): Seq[StoredAnalysis] = {
        if (!PREFERENCES.nodeExists("")) {
            return Seq.empty
        }
        var rua = Seq.empty[StoredAnalysis]
        for (i ← 1 to PREFERENCES.childrenNames().size; if PREFERENCES.nodeExists(s"$RECENT_ANALYSIS$i")) {
            val PREFERENCES_RECENT_ANALYSIS = PREFERENCES.node(s"$RECENT_ANALYSIS$i")
            val path = PREFERENCES_RECENT_ANALYSIS.get(s"$PREFERENCES_KEY_RECENT_ANALYSIS_FILE", "")
            val file = new File(path)
            if (file.exists) {
                val name = PREFERENCES_RECENT_ANALYSIS.get(s"$PREFERENCES_KEY_RECENT_ANALYSIS_NAME", "")
                val date = PREFERENCES_RECENT_ANALYSIS.get(s"$PREFERENCES_KEY_RECENT_ANALYSIS_DATE", "")
                val loadedAnalysis = StoredAnalysis(name, file, new Date(date.toLong))
                rua = rua :+ loadedAnalysis
            } else {
                // file moved or deleted, remove from Prefs
                PREFERENCES_RECENT_ANALYSIS.clear()
                PREFERENCES_RECENT_ANALYSIS.removeNode()
            }
        }
        rua
    }

    def deleteAnalyses(): Unit = {
        for (i ← 1 to PREFERENCES.childrenNames().size; if PREFERENCES.nodeExists(s"$RECENT_ANALYSIS$i")) {
            val PREFERENCES_RECENT_ANALYSIS = PREFERENCES.node(s"$RECENT_ANALYSIS$i")
            PREFERENCES_RECENT_ANALYSIS.clear()
            PREFERENCES_RECENT_ANALYSIS.removeNode()
        }
    }

    def main(args: Array[String]): Unit = {
        Application.launch(classOf[BugPicker], args: _*)
    }
}