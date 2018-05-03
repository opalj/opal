/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ai
package util

import scala.language.reflectiveCalls
import scala.xml.Node
import scala.xml.NodeSeq
import org.opalj.io.process
import org.opalj.br._
import org.opalj.collection.mutable.IntArrayStack

/**
 * Several utility methods to facilitate the development of the abstract interpreter/
 * new domains for the abstract interpreter, by creating various kinds of dumps of
 * the state of the interpreter.
 *
 * ==Thread Safety==
 * This object is thread-safe.
 *
 * @author Michael Eichberg
 */
object XHTML {

    def styles: String =
        process(this.getClass.getResourceAsStream("default.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    def jquery: String =
        process(this.getClass.getResourceAsStream("jquery-1.6.2.min.js"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    def colResizable: String =
        process(this.getClass.getResourceAsStream("colResizable-1.3.min.js"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    def createXHTML(
        htmlTitle: Option[String] = None,
        body:      NodeSeq
    ): Node = {
        val theTitle = htmlTitle.map(t ⇒ Seq(<title>{ t }</title>)).getOrElse(Seq.empty[Node])
        // HTML 5 XML serialization (XHTML 5)
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                { theTitle }
                <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8'/>
                <script type="text/javascript">NodeList.prototype.forEach = Array.prototype.forEach; </script>
                <script type="text/javascript">{ scala.xml.Unparsed(jquery) }</script>
                <script type="text/javascript">{ scala.xml.Unparsed(colResizable) }</script>
                <script type="text/javascript">$(function(){{$('table').colResizable({{ liveDrag:true, minWidth:75 }});}});</script>
                <style>{ scala.xml.Unparsed(styles) }</style>
            </head>
            <body>
                { body }
            </body>
        </html>
    }

    def caption(classFile: Option[ClassFile], method: Option[Method]): String = {
        val typeName = classFile.map(_.thisType.toJava).getOrElse("")
        val methodName = method.map(m ⇒ m.signatureToJava(false)).getOrElse("&lt; method &gt;")
        s"$typeName{ $methodName }"
    }

    def htmlify(s: String): Node = {
        scala.xml.Unparsed(
            s.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>").
                replace("\t", "&nbsp;&nbsp;")
        )
    }

    def valueToString(value: AnyRef)(implicit ids: Option[AnyRef ⇒ Int]): String = {
        value.toString + ids.map("@"+_.apply(value)).getOrElse("")
    }

    def throwableToXHTML(throwable: Throwable): scala.xml.Node = {
        val node =
            if (throwable.getStackTrace == null ||
                throwable.getStackTrace.size == 0) {
                <div>{ throwable.getClass.getSimpleName+" "+throwable.getMessage }</div>
            } else {
                val stackElements =
                    for { stackElement ← throwable.getStackTrace } yield {
                        <tr>
                            <td>{ stackElement.getClassName }</td>
                            <td>{ stackElement.getMethodName }</td>
                            <td>{ stackElement.getLineNumber }</td>
                        </tr>
                    }
                val summary = throwable.getClass.getSimpleName+" "+throwable.getMessage

                <details>
                    <summary>{ summary }</summary>
                    <table>{ stackElements }</table>
                </details>
            }

        if (throwable.getCause ne null) {
            val causedBy = throwableToXHTML(throwable.getCause)
            <div style="background-color:yellow">{ node } <p>caused by:</p>{ causedBy }</div>
        } else {
            node
        }
    }

    def instructionsToXHTML(
        title:        String,
        instructions: { def mkString(sep: String): String }
    ): Node = {
        <p>
            <span>{ title }: { instructions.mkString(", ") }</span>
        </p>
    }

    def evaluatedInstructionsToXHTML(evaluatedPCs: IntArrayStack) = {
        val header = "Evaluated instructions: "+evaluatedPCs.count(_ >= 0)+"<br>"
        val footer = ""
        val subroutineStart = "<details><summary>Subroutine</summary><div style=\"margin-left:2em;\">"
        val subroutineEnd = "</div></details>"

        var openSubroutines = 0
        val asStrings = evaluatedPCs.reverse.map {
            case SUBROUTINE_START ⇒
                openSubroutines += 1
                subroutineStart
            case SUBROUTINE_END ⇒
                openSubroutines -= 1
                subroutineEnd
            case instruction ⇒ instruction.toString+" "
        }

        header+"Evaluation Order:<br><div style=\"margin-left:2em;\">"+
            asStrings.mkString("") +
            (
                if (openSubroutines > 0) {
                    var missingSubroutineEnds = subroutineEnd
                    openSubroutines -= 1
                    while (openSubroutines > 0) {
                        missingSubroutineEnds += subroutineEnd
                        openSubroutines -= 1
                    }
                    missingSubroutineEnds
                } else
                    ""
            ) +
                footer+"</div>"
    }
}
