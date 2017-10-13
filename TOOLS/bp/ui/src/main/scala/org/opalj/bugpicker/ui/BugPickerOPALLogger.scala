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

import java.text.SimpleDateFormat;
import java.util.Date

import org.opalj.log.LogContext
import org.opalj.log.LogMessage
import org.opalj.log.OPALLogger

import scalafx.application.Platform
import scalafx.collections.ObservableBuffer

/**
 * The BugPicker logger is a logger that ignores the context and logs directly
 * on the BugPicker UI.
 *
 * @author David Becker
 */
class BugPickerOPALLogger(
        val messages: ObservableBuffer[BugPickerLogMessage]
) extends OPALLogger {

    def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
        Platform.runLater {
            messages +=
                BugPickerLogMessage(
                    BugPickerOPALLogger.DateFormatter.format(new Date()),
                    message.level.id,
                    message.category.getOrElse(""),
                    message.message
                )
        }
    }
}

private object BugPickerOPALLogger {
    final val DateFormatter = new SimpleDateFormat("HH:mm:ss:SSS")
}
