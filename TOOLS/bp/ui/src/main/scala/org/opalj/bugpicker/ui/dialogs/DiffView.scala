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
package ui
package dialogs

import java.io.File
import java.util.Date

import scala.io.Source
import scala.xml.Node
import scala.xml.Unparsed

import org.opalj.bugpicker.core.HTMLCSS
import org.opalj.bugpicker.core.analysis.BugPickerAnalysis.ReportCSS
import org.opalj.bugpicker.ui.BugPicker
import org.opalj.io.process

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

class DiffView(currentIssues: Iterable[Node], oldAnalysis: StoredAnalysis) extends Stage {
    self ⇒

    title = "Analysis Diff"

    scene = new Scene {
        root = new BorderPane {
            top = new VBox {
                margin = Insets(20)
                alignment = Pos.TopLeft
                children = Seq(
                    new Label("Analysis Difference between:"),
                    new Label("Now - "+currentIssues.size+" Issues"),
                    new Label(oldAnalysis.analysisName+" - "+oldAnalysis.getIssues.get.size+" Issues - From "+oldAnalysis.analysisDate)
                )
            }

            center = new WebView {
                contextMenuEnabled = false
                engine.loadContent(XHTMLContent.toString)
            }

            bottom = new HBox {
                children = new Button {
                    text = "Close"
                    defaultButton = true
                    HBox.setMargin(this, Insets(10))
                    onAction = { e: ActionEvent ⇒ close() }
                }
                alignment = Pos.Center
            }
        }
        stylesheets += BugPicker.defaultAppCSSURL
    }

    /**
     * expects two "issue"-nodes, as returned by `Issue.asXHTML`
     * @see ``StandardIssue.asXHTML``
     */
    def isSameIssue(n1: Node, n2: Node): Boolean = {
        def getData(n: Node) = {
            val description = n \ "dl" \ "dt"
            val data = n \ "dl" \ "dd"
            assert(description.size == data.size)
            (description.map(_.text.trim) zip (data.map(_.text.trim))).toMap
        }
        val data1 = getData(n1)
        val data2 = getData(n2)
        def eqIfExists(s: String) = {
            val c1 = data1.contains(s)
            val c2 = data2.contains(s)
            (c1 == c2 && // key must be in both or in neither
                (!c1 || data1(s) == data2(s))) // if it is in both, data of them has to be equal
        }
        (eqIfExists("class") &&
            eqIfExists("method") &&
            eqIfExists("instruction") &&
            eqIfExists("relevance"))
    }

    final lazy val DiffCSS: String =
        process(this.getClass.getResourceAsStream("diff.css"))(
            Source.fromInputStream(_).mkString
        )
    lazy val oldIssues = oldAnalysis.getIssues.get

    lazy val (commonIssues, newIssues) = currentIssues.partition(issue ⇒ {
        oldIssues.exists(isSameIssue(_, issue))
    })
    lazy val (_, missingIssues) = oldIssues.partition(issue ⇒
        currentIssues.exists(isSameIssue(_, issue)))

    lazy val XHTMLContent =
        <html>
            <head>
                <style>{ Unparsed(HTMLCSS) }</style>
                <style>{ Unparsed(ReportCSS) }</style>
                <style>{ Unparsed(DiffCSS) }</style>
            </head>
            <body>
                <div id="diff">
                    {
                        if (newIssues.nonEmpty) {
                            <details class="package_summary" open="true">
                                <summary class="package_summary">New Issue(s): { newIssues.size }</summary>
                                <div>{ newIssues }</div>
                            </details>
                        }
                    }
                    {
                        if (missingIssues.nonEmpty) {
                            <details class="package_summary" open="true">
                                <summary class="package_summary">Missing Issue(s): { missingIssues.size }</summary>
                                <div>{ missingIssues }</div>
                            </details>
                        }
                    }
                    {
                        if (commonIssues.nonEmpty) {
                            <details class="package_summary" open="true">
                                <summary class="package_summary">Common Issue(s): { commonIssues.size }</summary>
                                <div>{ commonIssues }</div>
                            </details>
                        }
                    }
                </div>
            </body>
        </html>

    def show(owner: Stage): Unit = {
        initModality(Modality.WINDOW_MODAL)
        initOwner(owner)
        initStyle(StageStyle.DECORATED)
        centerOnScreen
        showAndWait
    }
}

case class StoredAnalysis(
        analysisName: String,
        analysisFile: File,
        analysisDate: Date = java.util.Calendar.getInstance().getTime()) {
    lazy val xml = scala.xml.XML.loadFile(analysisFile)
    def getIssues(): Option[Iterable[Node]] = {
        try {
            val issues = xml \ "div"
            Some(issues)
        } catch {
            case e: Exception ⇒ None
        }
    }
    override def equals(a: Any): Boolean = {
        a.isInstanceOf[StoredAnalysis] && a.asInstanceOf[StoredAnalysis].analysisFile == analysisFile
    }
}

object StoredAnalysis {
    def readFromFile(file: File): Option[StoredAnalysis] = {
        try {
            val xml = scala.xml.XML.loadFile(file)
            val name = (xml \ "@name").text
            val date = (xml \ "@date").text
            Some(StoredAnalysis(name, file, new Date(date.toLong)))
        } catch {
            case e: Exception ⇒ None
        }
    }
}
