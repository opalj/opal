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
import scala.io.Source
import scala.xml.Unparsed

case class SourceFileWrapper(sourceFile: File, highlightLines: String) {
    private val source = Source.fromFile(sourceFile).mkString

    private val language: String = {
        val prefix = "language-"
        val name = sourceFile.getName
        val suffix = name.substring(name.lastIndexOf('.') + 1)
        prefix + suffix
    }

    def toXHTML: scala.xml.Node =
        <html>
            <head>
                <title>{ sourceFile.getName }</title>
                <style type="text/css">{ Unparsed(SourceFileWrapper.PRISM_CSS) }</style>
            </head>
            <body>
                <pre class="line-numbers" data-line={ highlightLines }><code class={ language }>{ source }</code></pre>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.PRISM_JS) }</script>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.ADD_LINE_ANCHORS) }</script>
            </body>
        </html>
}

object SourceFileWrapper {
    final val PRISM_JS_URL: String = "/org/opalj/bugpicker/codeview/prism.js"
    final lazy val PRISM_JS: String = Source.fromURL(getClass.getResource(PRISM_JS_URL)).mkString
    final val PRISM_CSS_URL: String = "/org/opalj/bugpicker/codeview/prism.css"
    final lazy val PRISM_CSS: String = Source.fromURL(getClass.getResource(PRISM_CSS_URL)).mkString
    final val ADD_LINE_ANCHORS_URL: String = "/org/opalj/bugpicker/codeview/add-line-anchors.js"
    final lazy val ADD_LINE_ANCHORS: String = Source.fromURL(getClass.getResource(ADD_LINE_ANCHORS_URL)).mkString
}
