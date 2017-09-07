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

import java.io.File
import java.net.URL
import scala.xml.{Node ⇒ xmlNode}
import scalafx.Includes.jfxWorkerStateEvent2sfxWorkerStateEvent
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.WorkerStateEvent
import scalafx.scene.control.ListView
import scalafx.scene.control.TabPane
import scalafx.scene.web.WebView
import scalafx.stage.Stage
import org.opalj.br.analyses.Project
import org.opalj.bugpicker.core.analyses.BugPickerAnalysis
import org.opalj.bugpicker.ui.codeview.AddClickListenersOnLoadListener
import org.opalj.bugpicker.ui.dialogs.DialogStage
import org.opalj.bugpicker.ui.dialogs.ProgressManagementDialog
import org.opalj.issues.Issue

/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author David Becker
 */
object AnalysisRunner extends BugPickerAnalysis {

    def runAnalysis(
        stage:   Stage,
        project: Project[URL], sources: Seq[File], issues: ObjectProperty[Iterable[Issue]],
        sourceView: WebView, byteView: WebView, reportView: WebView, tabPane: TabPane
    ): Unit = {

        if (project == null) {
            DialogStage.showMessage("Error", "You need to load a project first!", stage)
            reportView.engine.loadContent(Messages.LOAD_CLASSES_FIRST)
            return ;
        }

        val interrupted = BooleanProperty(false)

        val progressListView = new ListView[String] {
            prefHeight = ((Runtime.getRuntime().availableProcessors() + 2) * 24 + 2).toDouble
        }
        val progressListItems = scala.collection.mutable.HashMap[String, String]()
        val theProgress = DoubleProperty(0)
        val stepCount = project.projectClassFilesCount + BugPickerAnalysis.PreAnalysesCount

        val progStage =
            new ProgressManagementDialog(
                stage,
                reportView, progressListView, theProgress, stepCount, interrupted
            )

        val initProgressManagement =
            new InitProgressManagement(
                interrupted, theProgress,
                progressListView, progressListItems,
                stepCount.toDouble, progStage
            )

        val doc = new ObjectProperty[xmlNode]

        val worker = new AnalysisWorker(doc, project, issues, initProgressManagement)
        worker.handleEvent(WorkerStateEvent.ANY)(
            new WorkerFinishedListener(project, sources, doc, reportView, sourceView, byteView, tabPane)
        )

        worker.start
        progStage.centerOnScreen
        progStage.showAndWait
    }

    private class WorkerFinishedListener(
            project:    Project[URL],
            sources:    Seq[File],
            doc:        ObjectProperty[xmlNode],
            reportView: WebView,
            sourceView: WebView,
            byteView:   WebView,
            tabPane:    TabPane
    ) extends Function1[WorkerStateEvent, Unit] {

        override def apply(event: WorkerStateEvent): Unit = {
            event.eventType match {
                case WorkerStateEvent.WorkerStateSucceeded ⇒ {
                    reportView.engine.loadContent(doc().toString)
                    new AddClickListenersOnLoadListener(
                        project, sources, reportView, byteView, sourceView,
                        { view ⇒
                            if (view == sourceView) tabPane.selectionModel().select(2)
                            else if (view == byteView) tabPane.selectionModel().select(3)
                        }
                    )
                    byteView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                    sourceView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                }
                case WorkerStateEvent.WorkerStateRunning ⇒ {
                    reportView.engine.loadContent(Messages.ANALYSIS_RUNNING)
                }
                case _default ⇒ {
                    reportView.engine.loadContent(event.eventType.toString)
                }
            }
        }
    }
}
