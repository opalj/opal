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

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.stage.Window

class DialogStage(owner: Window) extends Stage {
    filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
        if (e.code == KeyCode.ESCAPE) {
            close()
        }
    }

    initModality(Modality.APPLICATION_MODAL)
    initStyle(StageStyle.DECORATED)
    initOwner(owner)
}

object DialogStage {

    def showMessage(theTitle: String, message: String, owner: Window) {
        val stage = new DialogStage(owner) {
            theStage ⇒
            title = theTitle
            scene = new Scene {
                root = new BorderPane {
                    center = new Label {
                        text = message
                        margin = Insets(20)
                    }
                    bottom = new HBox {
                        content = new Button {
                            text = "Close"
                            defaultButton = true
                            HBox.setMargin(this, Insets(10))
                            onAction = { e: ActionEvent ⇒
                                theStage.close()
                            }
                        }
                        alignment = Pos.CENTER
                    }
                }
                stylesheets += BugPicker.defaultStyles
            }
        }
        stage.showAndWait()
    }
}
