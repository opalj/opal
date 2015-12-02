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
package org.opalj.eclipse.actions

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.core.commands.ExecutionException
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.handlers.HandlerUtil
import org.opalj.eclipse.views.DisassemblerView
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.IViewPart
import org.opalj.eclipse.views.DisassemblerView
import org.opalj.eclipse.IJEProcessor

/**
 * @author Lukas Becker
 * @author Simon Bohlender
 * @author Simon Guendling
 * @author Felix Zoeller
 */
class DisassemblerActionHandler extends AbstractHandler {
    val h: IJEProcessor = new IJEProcessor()
    @throws(classOf[ExecutionException])
    def execute(event: ExecutionEvent): Object = {
        HandlerUtil.getCurrentSelection(event) match {
            case iss: IStructuredSelection ⇒
                for (element ← iss.toArray) {
                    element match {
                        case ije: IJavaElement ⇒
                            h.process(event, ije)
                        case _ ⇒
                    }
                }
            case _ ⇒
        }
        // return value is reserved for future use; must be null
        null
    }
}