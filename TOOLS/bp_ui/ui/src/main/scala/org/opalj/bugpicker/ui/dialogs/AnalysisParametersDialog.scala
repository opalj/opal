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

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.stage.Stage
import scalafx.scene.control.TextArea
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scalafx.scene.web.HTMLEditor

/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author David Becker
 * @author Michael Reif
 */
class AnalysisParametersDialog(owner: Stage) extends DialogStage(owner) {
    theStage ⇒

    title = "Set analysis parameters"

    var configuration: Option[Config] = None
    val buttonMinWidth = 80
    val buttonMargin = Insets(10)

    width = 640
    height = 800

    val configEditor = new TextArea {
        hgrow = Priority.Always
        vgrow = Priority.Always
        resizable_=(true)
        wrapText_=(true)
    }

    new HTMLEditor {
        contextMenu = (null)
        vgrow = Priority.Always
        hgrow = Priority.Always
    }

    scene = new Scene {
        root = new BorderPane {
            center = configEditor

            bottom = new HBox {
                children = Seq(
                    new Button {
                        text = "_Reset"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            configEditor.text = BugPicker.renderConfig(ConfigFactory.load())

                        }
                    },

                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒ close() }
                    },
                    new Button {
                        text = "_Save"
                        mnemonicParsing = true
                        defaultButton = true
                        minWidth = buttonMinWidth.toDouble
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            var interrupt = false
                            val configText = configEditor.text.getValue
                            val config = try {
                                ConfigFactory.parseString(configText)
                            } catch {
                                case _: Exception | _: Error ⇒ {
                                    DialogStage.showMessage(
                                        "Error",
                                        "You entered an invalid configuration Please make sure, that you use the HOCON-format!",
                                        theStage
                                    )
                                    interrupt = true
                                    ConfigFactory.load("")
                                }
                            }

                            if (!interrupt) {
                                configuration = Some(config)
                                close()
                            }
                        }
                    }
                )
                alignment = Pos.Center
            }
        }
    }

    def show(config: Config): Option[Config] = {
        configEditor.text = BugPicker.renderConfig(config)
        showAndWait()
        configuration
    }
}