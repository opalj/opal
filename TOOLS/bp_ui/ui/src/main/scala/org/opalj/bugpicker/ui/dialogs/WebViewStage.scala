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

import org.opalj.bugpicker.ui.BugPicker
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView
import scalafx.stage.Stage
import scalafx.stage.StageStyle

class WebViewStage extends Stage {

    filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
        if (e.code == KeyCode.Escape) {
            close()
        }
    }

    initStyle(StageStyle.Decorated)
}

object WebViewStage {

    def showWebView(
        theTitle: String,
        wv:       WebView,
        screenX:  Double,
        screenY:  Double
    ): Unit = {
        val stage = new WebViewStage {
            theStage ⇒
            title = theTitle
            x = screenX
            y = screenY
            scene = new Scene {
                root = new VBox {
                    prefWidth = 1060
                    children = Seq(wv)
                }
                stylesheets += BugPicker.defaultAppCSSURL
            }
        }
        stage.show()
    }
}
