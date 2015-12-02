/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

package org.opalj.eclipse.views

import org.eclipse.swt.SWT
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.browser.LocationEvent
import org.eclipse.swt.browser.LocationListener
import org.eclipse.swt.browser.ProgressEvent
import org.eclipse.swt.browser.ProgressListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.part.ViewPart

object DisassemblerView {
    val Id: String = "org.opalj.eclipse.views.DisassemblerView"
    val classNameToLinkJS: String =
        """|var regex = /((\w+\.)+[A-Z]\w*)/gm;
           |var replace = "<a class=\"classlink\" href=\"$1\">$1</a>"
           |var newBody = document.body.innerHTML.replace(regex, replace);
           |document.body.innerHTML = newBody""".stripMargin;
    val noContextMenuJS: String =
        """document.addEventListener('contextmenu', function(e) {e.preventDefault()});"""
}

/**
 * Eclipse view for displaying class file disassembly
 * @author Lukas Becker
 * @author Simon Bohlender
 * @author Simon Guendling
 * @author Felix Zoeller
 */
class DisassemblerView extends ViewPart {

    var browser: Browser = _

    def createPartControl(parent: Composite): Unit = {
        browser = new Browser(parent, SWT.NONE)
        browser.addProgressListener(new ProgressListener() {
            def changed(x$1: ProgressEvent): Unit = {}

            def completed(x$1: ProgressEvent): Unit = {
                browser.execute(DisassemblerView.classNameToLinkJS)
                browser.execute(DisassemblerView.noContextMenuJS)
            }
        })
        browser.addLocationListener(new LocationListener() {
            def changed(l: LocationEvent): Unit = {
            }
            def changing(l: LocationEvent): Unit = {
                if (l.location.startsWith("file://")) {
                    l.doit = false
                }
            }
        })
    }

    def setFocus(): Unit = {
        browser.setFocus
    }

    def setText(text: String): Unit = {
        browser.setText(text)

    }

    override def setPartName(text: String): Unit = {
        super.setPartName(text)
    }
}