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
package codeview

import java.io.File
import java.net.URL

import org.opalj.br.analyses.Project

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.scene.web.WebView

/**
 * Adds onClick listeners on `td` elements in `resultWebview`'s document (once it has finished loading).
 * Clicking them decompiles and opens the bytecode of the problem spot in `sourceWebview`.
 * Once the onClick listeners have been added, this listener unregisters itself from `resultWebview`.
 *
 * @param focus This function takes either bytecodeWebview or sourceWebview and ensures that the respective tab is focused
 */
class AddClickListenersOnLoadListener(
        project: Project[URL],
        sources: Seq[File],
        resultWebview: WebView,
        bytecodeWebview: WebView,
        sourceWebview: WebView,
        focus: WebView ⇒ Unit) extends ChangeListener[State] {

    private val loadWorker = resultWebview.engine.delegate.getLoadWorker

    loadWorker.stateProperty.addListener(this)

    override def changed(observable: ObservableValue[_ <: State], oldValue: State, newValue: State) {
        if (newValue != State.SUCCEEDED) return

        val document = resultWebview.engine.document
        val nodes = document.getElementsByTagName("span")

        for {
            i ← (0 to nodes.getLength)
            node = nodes.item(i)
            if node != null && node.getAttributes() != null &&
                node.getAttributes().getNamedItem("data-class") != null
        } {
            val eventTarget = node.asInstanceOf[org.w3c.dom.events.EventTarget]
            val listener = new DOMNodeClickListener(project, sources, node, bytecodeWebview, sourceWebview, focus)
            eventTarget.addEventListener("click", listener, false)
        }

        loadWorker.stateProperty.removeListener(this)
    }
}
