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

import org.opalj.bugpicker.BugPicker
import org.opalj.bugpicker.Messages

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.application.Platform
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.ReadOnlyDoubleProperty
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.geometry.Pos.sfxEnum2jfx
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.web.WebView
import scalafx.scene.web.WebView.sfxWebView2jfx
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

class ProgressManagementDialog(
        owner: Stage,
        reportView: WebView,
        progressListView: ListView[String],
        theProgress: ReadOnlyDoubleProperty,
        interrupted: BooleanProperty) extends Stage {

    theStage ⇒
    title = "Analysis Progress "
    width = 800

    val cancelAnalysisAndCloseWindow: () ⇒ Unit = () ⇒ {
        reportView.engine.loadContent(Messages.ANALYSES_CANCELLING)
        val listener = new ChangeListener[State] {
            override def changed(obs: ObservableValue[_ <: State], oldValue: State, newValue: State) {
                Platform.runLater {
                    interrupted() = true
                    theStage.close
                }
                reportView.getEngine.getLoadWorker.stateProperty.removeListener(this)
            }
        }
        reportView.getEngine.getLoadWorker.stateProperty.addListener(listener)
    }

    scene = new Scene {
        root = new BorderPane {
            top = new HBox {
                content = new ProgressBar {
                    progress <== theProgress
                    margin = Insets(5)
                    HBox.setHgrow(this, Priority.ALWAYS)
                    prefWidth <== theStage.width - 20
                    prefHeight = 30
                }
                alignment = Pos.CENTER
                hgrow = Priority.ALWAYS
            }
            center = progressListView
            bottom = new Button {
                id = "Cancel"
                text = "Cancel"
                minWidth = 80
                defaultButton = true
                onAction = { e: ActionEvent ⇒ cancelAnalysisAndCloseWindow() }
                BorderPane.setAlignment(this, Pos.CENTER)
                BorderPane.setMargin(this, Insets(10))
            }
        }
        stylesheets += BugPicker.defaultStyles
        filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
            if (e.code.equals(KeyCode.ESCAPE)) cancelAnalysisAndCloseWindow()
        }
    }

    initModality(Modality.WINDOW_MODAL)
    initOwner(owner)
    initStyle(StageStyle.DECORATED)
}
