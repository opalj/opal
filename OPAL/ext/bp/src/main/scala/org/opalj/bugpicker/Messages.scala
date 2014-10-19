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

import scala.io.Source

object Messages {
    def getMessage(path: String): String = process(getClass.getResourceAsStream(path))(Source.fromInputStream(_).mkString)

    final val ANALYSIS_RUNNING = getMessage("/org/opalj/bugpicker/messages/analysisrunning.html")
    final val ANALYSIS_FINISHED = getMessage("/org/opalj/bugpicker/messages/analysisfinished.html")
    final val LOADING_FINISHED = getMessage("/org/opalj/bugpicker/messages/projectloadingfinished.html")
    final val APP_STARTED = getMessage("/org/opalj/bugpicker/messages/appstarted.html")
    final val LOADING_STARTED = getMessage("/org/opalj/bugpicker/messages/projectloadingstarted.html")
    final val ANALYSES_CANCELLING = getMessage("/org/opalj/bugpicker/messages/analysescancelling.html")
    final val LOAD_CLASSES_FIRST = getMessage("/org/opalj/bugpicker/messages/loadclassesfirst.html")
    final val NO_BYTECODE_FOUND = getMessage("/org/opalj/bugpicker/messages/nobytecodefound.html")
    final val GET_HELP = getMessage("/org/opalj/bugpicker/messages/gethelp.html")

    val helpTopics: Seq[HelpTopic] = Seq(
        new HelpTopic(GET_HELP),
        new HelpTopic(APP_STARTED),
        new HelpTopic(LOADING_FINISHED),
        new HelpTopic(ANALYSIS_FINISHED)
    )
}

class HelpTopic(val content: String) {
    val title = "<h2>(.*)</h2>".r.findFirstMatchIn(content).map(_.group(1)).getOrElse("Untitled")

    override def toString: String = title
}
