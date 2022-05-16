/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
        val theTitle = htmlTitle.map(t => Seq(<title>{ t }</title>)).getOrElse(Seq.empty[Node])
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
        val methodName = method.map(m => m.signatureToJava(false)).getOrElse("&lt; method &gt;")
        s"$typeName{ $methodName }"
    }

    def htmlify(s: String): Node = {
        scala.xml.Unparsed(
            s.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>").
                replace("\t", "&nbsp;&nbsp;")
        )
    }

    def valueToString(value: AnyRef)(implicit ids: Option[AnyRef => Int]): String = {
        if (value != null)
            value.toString + ids.map("@"+_.apply(value)).getOrElse("")
        else
            "null@<N/A-value>"
    }

    def throwableToXHTML(throwable: Throwable): scala.xml.Node = {
        val node =
            if (throwable.getStackTrace == null ||
                throwable.getStackTrace.size == 0) {
                <div>{ throwable.getClass.getSimpleName+" "+throwable.getMessage }</div>
            } else {
                val stackElements =
                    for { stackElement <- throwable.getStackTrace } yield {
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
            case SUBROUTINE_START =>
                openSubroutines += 1
                subroutineStart
            case SUBROUTINE_END =>
                openSubroutines -= 1
                subroutineEnd
            case instruction => instruction.toString+" "
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
