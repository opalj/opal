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
package dialogs

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Date

import scala.io.Source
import scala.xml.Node
import scala.xml.Unparsed
import scala.collection._

import org.opalj.bugpicker.core.HTMLCSS
import org.opalj.bugpicker.core.ReportCSS
import org.opalj.bugpicker.ui.BugPicker
import org.opalj.bugpicker.ui.Messages
import org.opalj.io.process

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxWorker2sfxWorker
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.beans.property.DoubleProperty
import scalafx.concurrent.Service
import scalafx.concurrent.Task
import scalafx.concurrent.Task.sfxTask2jfx
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ProgressBar
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebEngine.sfxWebEngine2jfx
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

//FIXME currentParamters are not used anymore
class DiffView(
        currentName:       String,
        currentIssues:     Iterable[Node],
        currentParameters: Seq[String]    = Seq.empty,
        oldAnalysis:       StoredAnalysis
) extends Stage {
    self ⇒

    showing.onChange((_, _, newShow) ⇒ {
        if (newShow) {
            val generatingTask = Task[Unit](XHTMLContent.toString)
            val generatingService = Service(generatingTask)
            generatingService.running.onChange((_, _, isGenerating) ⇒
                if (!isGenerating) {
                    view.engine.getLoadWorker.running.onChange((_, _, isLoading) ⇒
                        if (!isLoading) {
                            saveButton.disable = false
                            loadProgress.close
                        })
                    view.engine.loadContent(XHTMLContent.toString)
                })
            generatingService.start
            loadProgress.showAndWait()
        }
    })

    val view =
        new WebView {
            contextMenuEnabled = false
            engine.loadContent(Messages.GENERATING_DIFF)
        }
    val saveButton = new Button {
        text = "Save"
        defaultButton = false
        disable = true
        HBox.setMargin(this, Insets(10))
        onAction = { e: ActionEvent ⇒ saveDiff() }
    }

    lazy val loadProgress = new Stage {
        initOwner(self)
        scene = new Scene {
            root = new StackPane {
                children = Seq(
                    new ProgressBar {
                        progress <== theProgress
                        margin = Insets(1)
                        HBox.setHgrow(this, Priority.Always)
                        minWidth <== (self.width / 2)
                        minHeight = 30
                    }
                )
                alignment = Pos.Center
                hgrow = Priority.Always
                resizable = false
            }
        }
    }

    title = "Analysis Diff"

    scene = new Scene {
        root = new BorderPane {
            top = new VBox {
                margin = Insets(20)
                alignment = Pos.TopLeft
                children = Seq(
                    new Label("Difference between analyses:")
                )
            }

            center = view

            bottom = new HBox {
                children = Seq(new Button {
                    text = "Close"
                    defaultButton = true
                    HBox.setMargin(this, Insets(10))
                    onAction = { e: ActionEvent ⇒ close() }
                }, saveButton)
                alignment = Pos.Center
            }
        }
        stylesheets += BugPicker.defaultAppCSSURL
    }

    final lazy val DiffCSS: String =
        process(this.getClass.getResourceAsStream("diff.css"))(
            Source.fromInputStream(_).mkString
        )

    def saveDiff() = {
        val fsd = new FileChooser {
            title = "Save Analysis"
            extensionFilters ++= Seq(
                new FileChooser.ExtensionFilter("HTML File", "*.html"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            )
            initialDirectory = BugPicker.loadLastDirectoryFromPreferences()
            initialFileName = "diff.html"
        }
        val file = fsd.showSaveDialog(self)
        if (file != null) {
            process { Files.newBufferedWriter(file.toPath, StandardCharsets.UTF_8) } { fos ⇒
                fos.write(XHTMLContent.toString, 0, XHTMLContent.toString.length)
            }
        }
    }

    lazy val theProgress = DoubleProperty(0.0)

    def updateProgress(c: Int, a: Int) = {
        val p: Double = (c + 1).toDouble / a.toDouble
        theProgress.update(p)
    }

    def getDiff(): (Iterable[Node], Iterable[Node], Iterable[Node]) = {
        val savedData1 = mutable.Map.empty[Int, Map[String, String]]
        val savedData2 = mutable.Map.empty[Int, Map[String, String]]
        val oIssues = oldAnalysis.getIssues.get.toSeq
        val cIssues = currentIssues.toSeq
        def isSameIssue(i: Int, j: Int): Boolean = {
            def getData(n: Node) = {
                val description = n \ "dl" \ "dt"
                val data = n \ "dl" \ "dd"
                assert(description.size == data.size)
                (description.map(_.text.trim) zip (data.map(_.text.trim))).toMap
            }
            if (!savedData1.isDefinedAt(i))
                savedData1 += (i → getData(cIssues(i)))
            if (!savedData2.isDefinedAt(j))
                savedData2 += (j → getData(oIssues(j)))
            val data1 = savedData1(i)
            val data2 = savedData1(j)
            def eqIfExists(s: String) = {
                val c1 = data1.contains(s)
                val c2 = data2.contains(s)
                (c1 == c2 && // key must be in both or in neither
                    (!c1 || (data1(s) == data2(s)))) // if it is in both, data of them has to be equal
            }
            eqIfExists("class") && eqIfExists("method") && eqIfExists("instruction") && eqIfExists("relevance")
        }

        var i = 0
        var j = 0

        var commonIssues = Seq.empty[Node]
        var newIssues = Seq.empty[Node]
        var missingIssues = Seq.empty[Node]
        while (i < cIssues.size && j < oIssues.size) {
            updateProgress(i, cIssues.size)
            val c1 = cIssues(i)
            if (isSameIssue(i, j)) { // both issues where they are expected, easy ...
                commonIssues = commonIssues :+ c1
                i += 1
                j += 1
            } else {
                var x = j
                var found = false
                while (!found && x < oIssues.size) {
                    if (isSameIssue(i, x)) { // found the same issue in both, but old one later than expected
                        commonIssues = commonIssues :+ c1 // add common issue
                        for (s ← j until x) { // everything between add as missing issues
                            missingIssues = missingIssues :+ oIssues(s)
                        }
                        j = x + 1
                        i += 1
                        found = true // cancel loop
                    }
                    x += 1
                }
                if (!found) { // did not find c1 in old issues, must be new
                    newIssues = newIssues :+ c1
                    i += 1
                }
            }
        }
        while (i == cIssues.size && j < oIssues.size) {
            missingIssues = missingIssues :+ oIssues(j)
            j += 1
        }
        while (i < cIssues.size && j == oIssues.size) {
            newIssues = newIssues :+ cIssues(i)
            i += 1
        }
        (newIssues, commonIssues, missingIssues)
    }

    lazy val (newIssues, commonIssues, missingIssues) = getDiff()

    lazy val XHTMLContent =
        <html>
            <head>
                <style>{ Unparsed(HTMLCSS) }</style>
                <style>{ Unparsed(ReportCSS) }</style>
                <style>{ Unparsed(DiffCSS) }</style>
            </head>
            <body>
                <div id="parameters">
                    <details id="analysis_parameters_summary" open="true">
                        <summary>Parameters</summary>
                        <table>
                            <tr>
                                <td> </td><td>{ currentName }</td><td>{ oldAnalysis.analysisName }</td>
                            </tr>
                            <tr>
                                <td>Issues</td><td>{ currentIssues.size.toString }</td><td>{ oldAnalysis.getIssues.getOrElse(Seq.empty).size.toString }</td>
                            </tr>
                            <tr>
                                <td>Date</td><td>{ java.util.Calendar.getInstance().getTime().toString }</td><td>{ oldAnalysis.analysisDate.toString }</td>
                            </tr>
                            {
                                (currentParameters zip oldAnalysis.getParameters.getOrElse(Seq.empty)).map {
                                    _ match {
                                        case (newP: String, oldP: String) ⇒
                                            val n = newP.split("=")
                                            val o = oldP.split("=")
                                            val name = n(0)
                                            val newParam = n(1)
                                            val oldParam = o(1)
                                            <tr class={ if (newParam == oldParam) "paramEqual" else "paramUnequal" }>
                                                <td>{ name }</td><td>{ newParam }</td><td>{ oldParam }</td>
                                            </tr>
                                    }
                                }
                            }
                        </table>
                    </details>
                </div>
                <div id="diff">
                    {
                        if (newIssues.nonEmpty) {
                            <details class="package_summary newIssues" open="true">
                                <summary class="package_summary">New Issue(s): { newIssues.size }</summary>
                                <div>{ newIssues }</div>
                            </details>
                        }
                    }
                    {
                        if (missingIssues.nonEmpty) {
                            <details class="package_summary missingIssues" open="true">
                                <summary class="package_summary">Missing Issue(s): { missingIssues.size }</summary>
                                <div>{ missingIssues }</div>
                            </details>
                        }
                    }
                    {
                        if (commonIssues.nonEmpty) {
                            <details class="package_summary">
                                <summary class="package_summary">Common Issue(s): { commonIssues.size }</summary>
                                <div>{ commonIssues }</div>
                            </details>
                        }
                    }
                </div>
            </body>
        </html>

    def show(owner: Stage): Unit = {
        initModality(Modality.WindowModal)
        initOwner(owner)
        initStyle(StageStyle.Decorated)
        centerOnScreen
        showAndWait
    }
}

case class StoredAnalysis(
        analysisName: String,
        analysisFile: File,
        analysisDate: Date   = java.util.Calendar.getInstance().getTime()
) {
    lazy val xml = scala.xml.XML.loadFile(analysisFile)
    def getIssues(): Option[Iterable[Node]] = {
        try {
            val issues = xml \ "issues" \ "div"
            Some(issues)
        } catch {
            case e: Exception ⇒ None
        }
    }
    def getParameters(): Option[Seq[String]] = {
        try {
            val parameters = xml \ "parameters" \ "parameter"
            Some(parameters.toSeq.map(_.text.trim))
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
