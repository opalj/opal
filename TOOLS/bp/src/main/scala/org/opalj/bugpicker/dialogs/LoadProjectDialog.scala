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
package dialogs

import java.io.File
import scala.collection.mutable.ListBuffer
import org.opalj.bugpicker.BugPicker
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.scene.control.TitledPane
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.DirectoryChooser
import scalafx.stage.FileChooser
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.scene.layout.BorderPane

class LoadProjectDialog(preferences: Option[LoadedFiles]) extends Stage {
    private final val buttonWidth = 200
    private final val buttonMargin = Insets(5)

    private final val boxMargin = Insets(5)
    private final val boxPadding = Insets(5)

    val jars = ListBuffer[File]() ++ preferences.map(_.projectFiles).getOrElse(Seq.empty)
    val sources = ListBuffer[File]() ++ preferences.map(_.projectSources).getOrElse(Seq.empty)
    val libs = ListBuffer[File]() ++ preferences.map(_.libraries).getOrElse(Seq.empty)

    var cancelled = false
    val jarListview = new ListView[String] {
        items() ++= jars.map(_.toString)
        hgrow = Priority.ALWAYS
    }
    val libsListview = new ListView[String] {
        items() ++= libs.map(_.toString)
        hgrow = Priority.ALWAYS
    }
    val sourceListview = new ListView[String] {
        items() ++= sources.map(_.toString)
        hgrow = Priority.ALWAYS
    }

    val self = this

    title = "Load project files"
    width = 800
    height = 600
    maxWidth = 800
    maxHeight = 600
    scene = new Scene {
        root = new BorderPane {
            center = new VBox {
                content = Seq(
                    new TitledPane {
                        text = "Select project files and directories"
                        collapsible = false
                        padding = boxPadding
                        margin = boxMargin

                        content = new HBox {
                            content = Seq(
                                jarListview,
                                new VBox {
                                    content = Seq(
                                        new Button {
                                            text = "Add _jar/class file"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val fcb = new FileChooser {
                                                    title = "Select file"
                                                }
                                                fcb.extensionFilters.addAll(
                                                    new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                                    new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                                val file = fcb.showOpenDialog(scene().getWindow())
                                                if (file != null) {
                                                    jars += file
                                                    jarListview.items.get.add(file.toString())
                                                }

                                            }
                                        },
                                        new Button {
                                            text = "Add _directory"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val dc = new DirectoryChooser {
                                                    title = "Select directory"
                                                }
                                                val file = dc.showDialog(scene().window())
                                                if (file != null) {
                                                    jars += file
                                                    jarListview.items() += file.toString()
                                                }
                                            }
                                        },
                                        new Button {
                                            text = "_Remove"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val selection = jarListview.selectionModel().getSelectedIndices
                                                if (!selection.isEmpty()) {
                                                    selection.reverse.foreach { i ⇒
                                                        if (jars.isDefinedAt(i)) jars.remove(i)
                                                    }
                                                    jarListview.items().clear()
                                                    jarListview.items() ++= jars.map(_.toString)
                                                }
                                            }
                                        })
                                })
                        }
                    },
                    new TitledPane {
                        text = "Select source directories"
                        collapsible = false
                        padding = boxPadding
                        margin = boxMargin

                        content = new HBox {
                            content = Seq(
                                sourceListview,
                                new VBox {
                                    content = Seq(
                                        new Button {
                                            text = "Add direc_tory"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val dc = new DirectoryChooser {
                                                    title = "Open Dialog"
                                                }
                                                val file = dc.showDialog(scene().window())
                                                if (file != null) {
                                                    sources += file
                                                    sourceListview.items() += file.toString()
                                                }
                                            }
                                        },
                                        new Button {
                                            text = "Re_move"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val selection = sourceListview.selectionModel().getSelectedIndices
                                                if (!selection.isEmpty()) {
                                                    selection.reverse.foreach { i ⇒
                                                        if (sources.isDefinedAt(i)) sources.remove(i)
                                                    }
                                                    sourceListview.items().clear()
                                                    sourceListview.items() ++= sources.map(_.toString)
                                                }
                                            }
                                        })
                                })
                        }
                    },
                    new TitledPane {
                        text = "Select library files and directories"
                        collapsible = false
                        padding = boxPadding
                        margin = boxMargin

                        content = new HBox {
                            content = Seq(
                                libsListview,
                                new VBox {
                                    content = Seq(
                                        new Button {
                                            text = "Add j_ar/class file"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val fcb = new FileChooser {
                                                    title = "Select file"
                                                }
                                                fcb.extensionFilters.addAll(
                                                    new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                                    new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                                val file = fcb.showOpenDialog(scene().getWindow())
                                                if (file != null) {
                                                    libs += file
                                                    libsListview.items() += file.toString()
                                                }

                                            }
                                        },
                                        new Button {
                                            text = "Add d_irectory"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val fcb = new DirectoryChooser {
                                                    title = "Select directory"
                                                }
                                                val file = fcb.showDialog(scene().getWindow())
                                                if (file != null) {
                                                    libs += file
                                                    libsListview.items() += file.toString()
                                                }

                                            }
                                        },
                                        new Button {
                                            text = "R_emove"
                                            mnemonicParsing = true
                                            maxWidth = buttonWidth
                                            minWidth = buttonWidth
                                            margin = buttonMargin
                                            onAction = { e: ActionEvent ⇒
                                                val selection = libsListview.selectionModel().getSelectedIndices
                                                if (!selection.isEmpty()) {
                                                    selection.reverse.foreach { i ⇒
                                                        if (libs.isDefinedAt(i)) libs.remove(i)
                                                    }
                                                    libsListview.items().clear()
                                                    libsListview.items() ++= libs.map(_.toString())
                                                }
                                            }
                                        })
                                })
                        }
                    })
            }
            bottom = new HBox {
                alignment = Pos.CENTER
                padding = boxPadding
                margin = boxMargin

                content = Seq(
                    new Button {
                        text = "C_lear"
                        mnemonicParsing = true
                        margin = buttonMargin
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒
                            jars.clear()
                            jarListview.items().clear()
                            libs.clear()
                            libsListview.items().clear()
                            sources.clear()
                            sourceListview.items().clear()
                        }
                    },
                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        margin = buttonMargin
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒
                            cancelled = true
                            self.close()
                        }
                    },
                    new Button {
                        text = "_Finish"
                        mnemonicParsing = true
                        margin = buttonMargin
                        defaultButton = true
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒ self.close() }
                    }
                )

            }
        }
        stylesheets += BugPicker.defaultStyles

        filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
            if (e.code.equals(KeyCode.ESCAPE)) {
                cancelled = true
                close()
            } else if (e.code.equals(KeyCode.ENTER)) {
                close()
            }
        }
    }

    def show(owner: Stage): Option[LoadedFiles] = {
        initModality(Modality.WINDOW_MODAL)
        initOwner(owner)
        initStyle(StageStyle.DECORATED)
        centerOnScreen
        showAndWait
        if (cancelled) {
            None
        } else {
            Some(LoadedFiles(
                projectFiles = jars,
                projectSources = sources,
                libraries = libs))
        }
    }
}

case class LoadedFiles(
    projectFiles: Seq[File] = Seq.empty,
    projectSources: Seq[File] = Seq.empty,
    libraries: Seq[File] = Seq.empty)
