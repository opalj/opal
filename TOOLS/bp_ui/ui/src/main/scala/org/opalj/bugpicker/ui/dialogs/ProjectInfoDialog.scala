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
import java.net.URL

import org.opalj.br.analyses.Project

import scala.xml.Unparsed

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.web.WebView
import scalafx.stage.Stage
import org.opalj.br.analyses.PropertyStoreKey

object ProjectInfoDialog {
    def toUL(files: Seq[File]): String =
        files.map(_.getAbsolutePath).mkString("<ul><li>", "</li><li>", "</li></ul>")

    def show(owner: Stage, project: Project[URL], sources: Seq[File]): Unit = {
        if (project == null) {
            DialogStage.showMessage(
                "Error",
                "You need to load a project before you can get information about it.",
                owner
            )
            return ;
        }

        val preferences = BugPicker.loadFilesFromPreferences().getOrElse(LoadedFiles())

        val html = report(project, preferences)

        val stage = new DialogStage(owner) {
            theStage ⇒
            scene = new Scene {
                root = new BorderPane {
                    center = new WebView {
                        contextMenuEnabled = false
                        engine.loadContent(html)
                    }
                    bottom = new HBox {
                        children = new Button {
                            text = "Close"
                            defaultButton = true
                            onAction = { e: ActionEvent ⇒ theStage.close() }
                            HBox.setMargin(this, Insets(10))
                        }
                        alignment = Pos.Center
                    }
                }
                stylesheets += BugPicker.defaultAppCSSURL
            }
        }

        stage.title = "Project info"
        stage.showAndWait
    }

    private def report(project: Project[URL], preferences: LoadedFiles): String = {

        val propertiesStoreInfo = project.get(PropertyStoreKey).toString()

        val mlStatistics =
            <table>
                <h2>Method Length Distribution</h2>
                <tr><th>Length of the Method</th><th>Count</th><th>Methods</th></tr>
                {
                    for { (length, methods) ← project.projectMethodsLengthDistribution } yield {
                        val count = methods.size
                        val method = methods.head
                        val methodId = method.toJava
                        val methodsInfo = if (count == 1) methodId else methodId+", ..."
                        <tr><td>{ length }</td><td>{ count }</td><td>{ methodsInfo }</td></tr>
                    }
                }
            </table>

        val cmpcdStatistics =
            <table>
                <h2>Class Members Per Class Distribution</h2>
                <tr><th>Number of Class Members</th><th>Count</th><th>Classes</th></tr>
                {
                    for { (size, (count, classes)) ← project.projectClassMembersPerClassDistribution } yield {
                        val classesInfo =
                            if (count <= 2)
                                classes.mkString(", ")
                            else
                                classes.take(2).mkString("", ", ", ", ...")
                        <tr><td>{ size }</td><td>{ count }</td><td>{ classesInfo }</td></tr>
                    }
                }
            </table>

        val css = Unparsed(
            """ |body {
	            |    font: 14px sans-serif;
                |}
                |ul, li {
                |    list-style-type: none;
                |    padding-left: 0;
                |}
                |table { border: 1px solid gray; }
                |tr:nth-child(even) {background: #CCC}
                |tr:nth-child(odd) {background: #FFF}""".stripMargin('|')
        )

        <html>
            <head>
                <style type="text/css">{ css }</style>
            </head>
            <body>
                <h1>Project statistics</h1>
                <ul>{ project.statistics.toList.map(e ⇒ e._1+": "+e._2).sorted.map(e ⇒ <li>{ e }</li>) }</ul>
                <h2>Properties information</h2>
                <ul>{ Unparsed(propertiesStoreInfo.replace("\n", "<br>").replace("\t", "&nbsp;&nbsp;")) }</ul>
                <h2>Loaded jar files and directories</h2>
                <ul>{ preferences.projectFiles.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Loaded libraries</h2>
                <ul>{ preferences.libraries.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Source directories</h2>
                <ul>{ preferences.projectSources.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                { cmpcdStatistics }
                { mlStatistics }
            </body>
        </html>.toString
    }
}
