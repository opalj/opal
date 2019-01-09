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
import scala.collection.mutable.ListBuffer
import org.opalj.bugpicker.ui.BugPicker
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.scene.control.TextField
import scalafx.scene.control.TitledPane
import scalafx.scene.input.Dragboard
import scalafx.scene.input.DragEvent
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.input.TransferMode
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.DirectoryChooser
import scalafx.stage.FileChooser
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.stage.WindowEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.ScrollPane
import com.typesafe.config.Config

class LoadProjectDialog(
        preferences:    Option[LoadedFiles],
        recentProjects: Seq[LoadedFiles]
) extends Stage {
    theStage ⇒

    private final val buttonWidth = 200
    private final val buttonMargin = Insets(5)

    private final val boxMargin = Insets(5)
    private final val boxPadding = Insets(5)

    val jars = ListBuffer[File]() ++ preferences.map(_.projectFiles).getOrElse(Seq.empty)
    val sources = ListBuffer[File]() ++ preferences.map(_.projectSources).getOrElse(Seq.empty)
    val libs = ListBuffer[File]() ++ preferences.map(_.libraries).getOrElse(Seq.empty)

    var cancelled = false
    val nameTextField = new TextField {
        text = preferences.map(_.projectName).getOrElse("")
        prefWidth = 778
    }
    val jarListview = new ListView[String] {
        items() ++= jars.map(_.toString)
        hgrow = Priority.Always
        selectionModel().selectionMode = SelectionMode.Multiple
        onDragOver = onDragOverBehaviour
        onDragDropped = onDragDroppedBehaviour(jars, this)
    }
    val sourceListview = new ListView[String] {
        items() ++= sources.map(_.toString)
        hgrow = Priority.Always
        selectionModel().selectionMode = SelectionMode.Multiple
        onDragOver = onDragOverBehaviour
        onDragDropped = onDragDroppedBehaviour(sources, this, false)
    }
    val libsListview = new ListView[String] {
        items() ++= libs.map(_.toString)
        hgrow = Priority.Always
        selectionModel().selectionMode = SelectionMode.Multiple
        onDragOver = onDragOverBehaviour
        onDragDropped = onDragDroppedBehaviour(libs, this)
    }
    val config = preferences.map { _.config }.getOrElse(None)

    val self = this

    title = "Load project files"
    width = 800
    height = 600
    maxWidth = 800
    maxHeight = 600
    scene = new Scene {
        root = new BorderPane {
            center = new ScrollPane {
                content = new VBox {
                    prefWidth = 778
                    children = Seq(
                        new TitledPane {
                            text = "Choose a project name"
                            collapsible = true
                            padding = boxPadding
                            margin = boxMargin
                            maxHeight = 70

                            content = new HBox {
                                children = Seq(
                                    nameTextField
                                )
                            }
                        },
                        new TitledPane {
                            text = "Select project files and directories"
                            collapsible = true
                            padding = boxPadding
                            margin = boxMargin
                            maxHeight = 220

                            content = new HBox {
                                children = Seq(
                                    jarListview,
                                    new VBox {
                                        children = Seq(
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
                                                        new FileChooser.ExtensionFilter("Class Files", "*.class"),
                                                        new FileChooser.ExtensionFilter("Zip Files", "*.zip")
                                                    )
                                                    val file = fcb.showOpenDialog(scene().getWindow())
                                                    if (file != null && !jars.contains(file)) {
                                                        if (jars.isEmpty && nameTextField.text == "") {
                                                            nameTextField.text = file.toString()
                                                        }
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
                                                    if (file != null && !jars.contains(file)) {
                                                        if (jars.isEmpty && nameTextField.text == "") {
                                                            nameTextField.text = file.toString()
                                                        }
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
                                            },
                                            new Button {
                                                text = "_Remove all"
                                                mnemonicParsing = true
                                                maxWidth = buttonWidth
                                                minWidth = buttonWidth
                                                margin = buttonMargin
                                                onAction = { e: ActionEvent ⇒
                                                    jarListview.items().clear()
                                                    jars.clear()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        },
                        new TitledPane {
                            text = "Select source directories"
                            collapsible = true
                            padding = boxPadding
                            margin = boxMargin
                            maxHeight = 220

                            content = new HBox {
                                children = Seq(
                                    sourceListview,
                                    new VBox {
                                        children = Seq(
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
                                                    if (file != null && !sources.contains(file)) {
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
                                            },
                                            new Button {
                                                text = "Re_move all"
                                                mnemonicParsing = true
                                                maxWidth = buttonWidth
                                                minWidth = buttonWidth
                                                margin = buttonMargin
                                                onAction = { e: ActionEvent ⇒
                                                    sourceListview.items().clear()
                                                    sources.clear()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        },
                        new TitledPane {
                            text = "Select library files and directories"
                            collapsible = true
                            padding = boxPadding
                            margin = boxMargin
                            maxHeight = 220

                            content = new HBox {
                                children = Seq(
                                    libsListview,
                                    new VBox {
                                        children = Seq(
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
                                                        new FileChooser.ExtensionFilter("Class Files", "*.class"),
                                                        new FileChooser.ExtensionFilter("Zip Files", "*.zip")
                                                    )
                                                    val file = fcb.showOpenDialog(scene().getWindow())
                                                    if (file != null && !libs.contains(file)) {
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
                                                    if (file != null && !libs.contains(file)) {
                                                        libs += file
                                                        libsListview.items() += file.toString()
                                                    }
                                                }
                                            },
                                            new Button {
                                                text = "Add _JRE"
                                                mnemonicParsing = true
                                                maxWidth = buttonWidth
                                                minWidth = buttonWidth
                                                margin = buttonMargin
                                                onAction = { e: ActionEvent ⇒
                                                    val jre = org.opalj.bytecode.JRELibraryFolder
                                                    if (!libs.contains(jre)) {
                                                        libs += jre
                                                        libsListview.items() += jre.toString
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
                                            },
                                            new Button {
                                                text = "R_emove all"
                                                mnemonicParsing = true
                                                maxWidth = buttonWidth
                                                minWidth = buttonWidth
                                                margin = buttonMargin
                                                onAction = { e: ActionEvent ⇒
                                                    libsListview.items().clear()
                                                    libs.clear()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            }
            bottom = new HBox {
                alignment = Pos.Center
                padding = boxPadding
                margin = boxMargin

                children = Seq(
                    new Button {
                        text = "C_lear"
                        mnemonicParsing = true
                        margin = buttonMargin
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒
                            nameTextField.clear()
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
                        Platform.runLater { requestFocus() }
                        onAction = { e: ActionEvent ⇒
                            if (nameTextField.text.value == "") {
                                DialogStage.showMessage(
                                    "Error",
                                    "You have not specified a name for the project!",
                                    theStage
                                )
                            } else if (jars.isEmpty) {
                                DialogStage.showMessage(
                                    "Error",
                                    "You have not specified any classes to be analyzed!",
                                    theStage
                                )
                            } else if (nameAlreadyExists) {
                                if (DialogStage.showMessageWithBinaryChoice(
                                    "Warning",
                                    "A project with the name \""+nameTextField.text.value+"\" already exists. Do you want to replace it?",
                                    "Cancel", "Replace", theStage
                                ))
                                    self.close()
                            } else {
                                self.close()
                            }
                        }
                    }
                )
            }
        }
        stylesheets += BugPicker.defaultAppCSSURL

        filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
            if (e.code.equals(KeyCode.Escape)) {
                cancelled = true
                self.close()
            }
        }
    }

    theStage.onCloseRequest = { e: WindowEvent ⇒
        cancelled = true
    }

    def onDragOverBehaviour(): DragEvent ⇒ Unit = { e: DragEvent ⇒
        val db: Dragboard = e.getDragboard()
        if (db.hasFiles())
            e.acceptTransferModes(TransferMode.Copy)
        else
            e.consume()
    }

    def onDragDroppedBehaviour(
        lb:          ListBuffer[File],
        lv:          ListView[String],
        acceptFiles: Boolean          = true
    ): DragEvent ⇒ Unit = { e: DragEvent ⇒
        val db: Dragboard = e.getDragboard()
        var success: Boolean = false
        if (db.hasFiles()) {
            success = true

            val illegalFiles =
                (for {
                    file ← db.files
                    if !lb.contains(file)
                } yield {
                    if (file.isDirectory()) {
                        lb += file
                        lv.items().add(file.toString())
                        None
                    } else /*file.isFile() */ {
                        if (acceptFiles) {
                            file.getName.split('.').lastOption match {
                                case Some("jar") | Some("zip") | Some("class") ⇒ {
                                    lb += file
                                    lv.items().add(file.toString())
                                    None
                                }
                                case _ ⇒
                                    Some(file)
                            }
                        } else {
                            Some(file)
                        }
                    }
                }).filter(_.isDefined).flatten
            if (illegalFiles.nonEmpty) {
                val msg =
                    (if (acceptFiles)
                        "You can only drag and drop directories or files of type .jar/.zip/.class\n\n"
                    else
                        "You can only drag and drop directories here.\n\n")+
                        "Because of that restriction, the following files weren't added:\n"+
                        illegalFiles.take(10).mkString("    ", "    \n    ", "") +
                        (if (illegalFiles.size > 10)
                            "\n    ... (further files)."
                        else
                            ".")

                DialogStage.showMessage("Warning", msg, theStage)
            }
        }
        e.dropCompleted = success
        e.consume
    }

    def nameAlreadyExists: Boolean = {
        recentProjects.exists(p ⇒ p.projectName == nameTextField.text.value &&
            // this guarantees that identical projects are still loaded
            !(p.projectFiles == jars && p.projectSources == sources &&
                p.libraries == libs) && p.config == config)
    }

    def show(owner: Stage): Option[LoadedFiles] = {
        initModality(Modality.WindowModal)
        initOwner(owner)
        initStyle(StageStyle.Decorated)
        centerOnScreen
        showAndWait
        if (cancelled) {
            None
        } else {
            Some(LoadedFiles(
                projectName = nameTextField.text.value,
                projectFiles = jars,
                projectSources = sources,
                libraries = libs,
                config = config
            ))
        }
    }
}

case class LoadedFiles(
        projectName:    String         = "",
        projectFiles:   Seq[File]      = Seq.empty,
        projectSources: Seq[File]      = Seq.empty,
        libraries:      Seq[File]      = Seq.empty,
        config:         Option[Config] = None
)
