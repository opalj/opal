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
import scalafx.Includes.jfxMouseEvent2sfx
import scalafx.Includes.jfxMultipleSelectionModel2sfx
import scalafx.Includes.jfxObjectProperty2sfx
import scalafx.Includes.jfxReadOnlyObjectProperty2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.ListView
import scalafx.scene.control.ListView.sfxListView2jfx
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.SplitPane
import scalafx.scene.input.MouseEvent
import scalafx.scene.web.WebView
import scalafx.scene.web.WebView.sfxWebView2jfx
import scalafx.stage.Stage
import scalafx.stage.StageStyle

object HelpBrowser extends Stage {
    title = "BugPicker Help"
    minWidth = 800
    minHeight = 600

    scene = new Scene {
        root = new SplitPane {
            val list = new ListView[HelpTopic] {
                items() ++= Messages.helpTopics
                selectionModel.delegate().selectionMode = SelectionMode.SINGLE
            }
            val browser = new WebView
            browser.contextMenuEnabled = false
            list.selectionModel.delegate().selectedItemProperty().onChange { (observable, oldValue, newValue) ⇒
                updateView(newValue, browser)
            }
            list.onMouseClicked = { e: MouseEvent ⇒
                val selectedTopic = list.selectionModel.delegate().selectedItem()
                updateView(selectedTopic, browser)
            }
            items ++= Seq(list, browser)
            dividerPositions = 0.3
        }
        stylesheets += BugPicker.defaultStyles
    }

    initStyle(StageStyle.DECORATED)

    private def updateView(topic: HelpTopic, browser: WebView) {
        browser.engine.loadContent(topic.content)
        title = s"BugPicker Help - ${topic.title}"
    }

    override def show() {
        centerOnScreen()
        super.show()
    }
}
