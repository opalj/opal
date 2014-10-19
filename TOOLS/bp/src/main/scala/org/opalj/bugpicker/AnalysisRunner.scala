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

import java.io.File
import java.net.URL
import scala.io.Source
import scala.xml.{ Node ⇒ xmlNode }
import org.opalj.ai.debug.XHTML
import org.opalj.br.analyses.Project
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.{ Service ⇒ jService }
import javafx.concurrent.{ Task ⇒ jTask }
import javafx.concurrent.Worker.State
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.DoubleProperty
import scalafx.concurrent.WorkerStateEvent
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.geometry.Pos.sfxEnum2jfx
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.concurrent.Service
import scalafx.beans.property.ObjectProperty
import org.opalj.bugpicker.analysis.BugPickerAnalysis
import org.opalj.bugpicker.dialogs.DialogStage
import org.opalj.bugpicker.codeview.AddClickListenersOnLoadListener
import org.opalj.bugpicker.dialogs.ProgressManagementDialog
import org.opalj.bugpicker.analysis.AnalysisParameters
import scalafx.scene.control.TabPane
import scalafx.scene.web.WebView

object AnalysisRunner extends BugPickerAnalysis {

    def runAnalysis(stage: Stage, project: Project[URL], sources: Seq[File], parameters: AnalysisParameters,
                    sourceView: WebView, byteView: WebView, reportView: WebView, tabPane: TabPane) {
        val scene: Scene = stage.scene()
        if (project == null) {
            DialogStage.showMessage("Error", "You need to load a project first!", stage)
            reportView.engine.loadContent(Messages.LOAD_CLASSES_FIRST)
            return
        }

        val interrupted = BooleanProperty(false)

        val progressListView = new ListView[String] {
            prefHeight = (Runtime.getRuntime().availableProcessors() + 2) * 24 + 2
        }
        val progressListItems = scala.collection.mutable.HashMap[String, String]()
        val theProgress = DoubleProperty(0)
        val classCount = project.classFilesCount.toDouble

        val progStage =
            new ProgressManagementDialog(
                stage,
                reportView, progressListView, theProgress,
                interrupted)

        val initProgressManagement =
            new InitProgressManagement(
                interrupted, theProgress,
                progressListView, progressListItems,
                classCount, progStage)

        val doc = new ObjectProperty[xmlNode]

        val worker = new AnalysisWorker(doc, project, parameters, initProgressManagement)
        worker.handleEvent(WorkerStateEvent.ANY)(
            new WorkerFinishedListener(project, sources, doc, reportView, sourceView, byteView, tabPane)
        )

        worker.start
        progStage.centerOnScreen
        progStage.showAndWait
    }

    private class WorkerFinishedListener(
            project: Project[URL],
            sources: Seq[File],
            doc: ObjectProperty[xmlNode],
            reportView: WebView,
            sourceView: WebView,
            byteView: WebView,
            tabPane: TabPane) extends Function1[WorkerStateEvent, Unit] {

        override def apply(event: WorkerStateEvent): Unit = {
            event.eventType match {
                case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                    reportView.engine.loadContent(doc().toString)
                    new AddClickListenersOnLoadListener(
                        project, sources, reportView, byteView, sourceView,
                        { view ⇒
                            if (view == sourceView) tabPane.selectionModel().select(0)
                            else if (view == byteView) tabPane.selectionModel().select(1)
                        }
                    )
                    byteView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                    sourceView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                }
                case WorkerStateEvent.WORKER_STATE_RUNNING ⇒ {
                    reportView.engine.loadContent(Messages.ANALYSIS_RUNNING)
                }
                case _default ⇒ {
                    reportView.engine.loadContent(event.eventType.toString)
                }
            }
        }
    }
}
