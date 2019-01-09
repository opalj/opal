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
package codeview

import scala.io.Source

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.scene.web.WebView

class JumpToProblemListener(
        webview:      WebView,
        methodOption: Option[String],
        pcOption:     Option[String],
        lineOption:   Option[String]
) extends ChangeListener[State] {

    val worker = webview.engine.delegate.getLoadWorker

    worker.stateProperty.addListener(this)

    def runScript(script: String): Unit =
        try {
            webview.engine.delegate.executeScript(script)
        } catch {
            case e: Exception ⇒
                System.err.println("failed to run the script:")
                System.err.println(script)
                throw e
        }

    override def changed(
        observable: ObservableValue[_ <: State],
        oldValue:   State,
        newValue:   State
    ): Unit = {

        if (newValue != State.SUCCEEDED) return

        val jumpCall =
            if (lineOption.isDefined) {
                s"jumpToLineInSourceCode(${lineOption.get})"
            } else if (methodOption.isDefined && !pcOption.isDefined) {
                s"jumpToMethodInBytecode('${methodOption.get}');"
            } else if (methodOption.isDefined && pcOption.isDefined) {
                s"jumpToProblemInBytecode('${methodOption.get}', ${pcOption.get});"
            } else {
                "window.scrollTo(0,0);"
            }
        runScript(JumpToProblemListener.JUMP_JS + jumpCall)

        worker.stateProperty.removeListener(this)
    }
}

object JumpToProblemListener {
    final val JUMP_JS_URL: String = "/org/opalj/bugpicker/ui/codeview/jump-to-problem.js"
    final lazy val JUMP_JS: String = Source.fromURL(getClass.getResource(JUMP_JS_URL)).mkString
}
