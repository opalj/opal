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

import org.opalj.ai.common.DomainRegistry
import org.opalj.bugpicker.ui.BugPicker

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.jfxToggle2sfx
import scalafx.Includes.jfxWindowEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.RadioButton
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ToggleGroup
import scalafx.scene.control.Tooltip
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.stage.WindowEvent

class ChooseDomainDialog extends Stage {
    theStage ⇒

    var cancelled = false

    private[this] final val defaultMargin = Insets(3)
    private[this] final val defaultPadding = Insets(3)

    private[this] val domainToggleGroup = new ToggleGroup()

    val self = this

    title = "Choose Domain"
    width = 300
    scene = new Scene {
        root = new BorderPane {
            center = new ScrollPane {
                content = new VBox {
                    padding = defaultPadding
                    margin = defaultMargin
                    children = for (domain ← DomainRegistry.domainDescriptions.toSeq.sorted) yield {
                        new RadioButton(domain.split(']')(0).drop(1)) {
                            toggleGroup = domainToggleGroup
                            mnemonicParsing = true
                            margin = defaultMargin
                            userData = domain
                            selected = domain.contains("basic domain")
                            tooltip = new Tooltip {
                                text = domain
                            }
                        }
                    }
                }
            }
            bottom = new HBox {
                alignment = Pos.Center
                padding = defaultPadding
                margin = defaultMargin

                children = Seq(
                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        margin = defaultMargin
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒
                            cancelled = true
                            self.close()
                        }
                    },
                    new Button {
                        text = "_Finish"
                        mnemonicParsing = true
                        margin = defaultMargin
                        defaultButton = true
                        minWidth = 80
                        Platform.runLater { requestFocus() }
                        onAction = { e: ActionEvent ⇒
                            self.close()
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

    def show(owner: Stage): Option[String] = {
        initModality(Modality.WindowModal)
        initOwner(owner)
        initStyle(StageStyle.Decorated)
        centerOnScreen
        showAndWait
        if (cancelled) {
            None
        } else {
            domainToggleGroup.selectedToggle.value.userData match {
                case s: String ⇒ Some(s)
            }
        }
    }
}