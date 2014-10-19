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

import scala.collection.JavaConversions
import java.io.File
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8LibraryFrameworkWithCaching
import org.opalj.br.ClassFile
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.geometry.Insets
import scalafx.scene.web.WebView
import scalafx.event.ActionEvent
import scala.language.implicitConversions
import scalafx.scene.Scene
import org.opalj.bugpicker.dialogs.DialogStage
import org.opalj.bugpicker.dialogs.LoadedFiles

object ProjectHelper {
    def setupProject(loadedFiles: LoadedFiles, parentStage: Stage): (Project[URL], Seq[File]) = {

        val files = loadedFiles.projectFiles
        val sources = loadedFiles.projectSources
        val libs = loadedFiles.libraries
        val project = setupProject(files, libs, parentStage)
        (project, sources)
    }

    def setupProject(
        cpFiles: Iterable[File],
        libcpFiles: Iterable[File],
        parentStage: Stage): Project[URL] = {
        println("[info] Reading class files (found in):")
        val cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
        val Java8ClassFileReader = new Java8FrameworkWithCaching(cache)
        val Java8LibraryClassFileReader = new Java8LibraryFrameworkWithCaching(cache)

        val (classFiles, exceptions1) =
            br.reader.readClassFiles(
                cpFiles,
                Java8ClassFileReader.ClassFiles,
                (file) ⇒ println("[info]\t"+file))

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                println("[info] Reading library class files (found in):")
                br.reader.readClassFiles(
                    libcpFiles,
                    Java8LibraryClassFileReader.ClassFiles,
                    (file) ⇒ println("[info]\t"+file))
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }
        val allExceptions = exceptions1 ++ exceptions2
        if (allExceptions.nonEmpty) {
            val out = new java.io.ByteArrayOutputStream
            val pout = new java.io.PrintStream(out)
            for (exception ← exceptions1 ++ exceptions2) {
                pout.println(s"<h3>${exception.getMessage}</h3>")
                exception.getStackTrace.foreach { ste ⇒
                    pout.append(ste.toString).println("<br/>")
                }
            }
            pout.flush
            val message = new String(out.toByteArray)
            val dialog = new DialogStage(parentStage) {
                title = "Error while reading project"
                scene = new Scene {
                    root = new BorderPane {
                        top = new Label {
                            text = "The following exceptions occurred while reading the specified files:"
                        }
                        center = new WebView {
                            contextMenuEnabled = false
                            engine.loadContent(message)
                        }
                        bottom = new HBox {
                            content = new Button {
                                text = "Close"
                                padding = Insets(5, 10, 5, 10)
                                onAction = { e: ActionEvent ⇒
                                    close()
                                }
                            }
                        }
                    }
                }
            }
            dialog.showAndWait()
        }

        var project = Project(classFiles, libraryClassFiles)
        print(
            project.statistics.map(kv ⇒ "- "+kv._1+": "+kv._2).toList.sorted.
                mkString("[info] Project statistics:\n[info]\t", "\n[info]\t", "\n")
        )
        project
    }
}
