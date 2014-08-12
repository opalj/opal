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
package ai
package debug

import java.awt.Desktop
import java.io.FileOutputStream
import java.io.File

import scala.language.existentials
import scala.util.control.ControlThrowable

import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Unparsed
import scala.xml.Text
import scala.xml.Unparsed

import br._
import br.instructions._

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

    private[this] val dumpMutex = new Object

    /**
     * Stores the time when the last dump was created.
     *
     * We generate dumps on errors only if the specified time has passed by to avoid that
     * we are drowned in dumps. Often, a single bug causes many dumps to be created.
     */
    private[this] var _lastDump = new java.util.concurrent.atomic.AtomicLong(0l)

    private[this] def lastDump_=(currentTimeMillis: Long) {
        _lastDump.set(currentTimeMillis)
    }

    private[this] def lastDump = _lastDump.get()

    def dumpOnFailure[T, D <: Domain](
        classFile: ClassFile,
        method: Method,
        ai: AI[_ >: D],
        theDomain: D,
        minimumDumpInterval: Long = 500l)(
            f: AIResult { val domain: theDomain.type } ⇒ T): T = {
        val result = ai(classFile, method, theDomain)
        val operandsArray = result.operandsArray
        val localsArray = result.localsArray
        try {
            if (result.wasAborted)
                throw new RuntimeException("interpretation aborted")
            f(result)
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case e: Throwable ⇒
                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastDump) > minimumDumpInterval) {
                    lastDump = currentTime
                    val title = Some("Generated due to exception: "+e.getMessage())
                    val dump =
                        XHTML.dump(
                            Some(classFile), Some(method), method.body.get,
                            title,
                            theDomain
                        )(operandsArray, localsArray)
                    XHTML.writeAndOpenDump(dump) //.map(_.deleteOnExit)
                } else {
                    Console.err.println("[info] dump suppressed: "+e.getMessage())
                }
                throw e
        }
    }

    /**
     * In case that during the validation some exception is thrown, a dump of
     * the current memory layout is written to a temporary file and opened in a
     * browser; the number of dumps that are generated is controlled by
     * `timeInMillisBetweenDumps`. If a dump is suppressed a short message is
     * printed on the console.
     *
     * @param f The funcation that performs the validation of the results.
     */
    def dumpOnFailureDuringValidation[T](
        classFile: Option[ClassFile],
        method: Option[Method],
        code: Code,
        result: AIResult,
        minimumDumpInterval: Long = 500l)(
            f: ⇒ T): T = {
        val operandsArray = result.operandsArray
        val localsArray = result.localsArray
        try {
            if (result.wasAborted) throw new RuntimeException("interpretation aborted")
            f
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case e: Throwable ⇒
                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastDump) > minimumDumpInterval) {
                    lastDump = currentTime
                    writeAndOpenDump(
                        dump(
                            classFile.get, method.get,
                            "Dump generated due to exception: "+e.getMessage(),
                            result)
                    )
                } else {
                    Console.err.println("dump suppressed: "+e.getMessage())
                }
                throw e
        }
    }

    def createXHTML(
        htmlTitle: Option[String] = None,
        body: NodeSeq): Node = {
        val theTitle = htmlTitle.map(t ⇒ Seq(<title>{ t }</title>)).getOrElse(Seq.empty[Node])
        // HTML 5 XML serialization (XHTML 5)
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                { theTitle }
                <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8'/>
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

    def dump(
        classFile: ClassFile,
        method: Method,
        resultHeader: String,
        result: AIResult): Node = {
        import result._

        val title = s"${classFile.thisType.toJava}{ ${method.toJava} }"

        createXHTML(
            Some(title),
            Seq[Node](
                <h1>{ title }</h1>,
                annotationsAsXHTML(method),
                scala.xml.Unparsed(resultHeader),
                dumpTable(code, domain)(operandsArray, localsArray)))
    }

    def annotationsAsXHTML(method: Method) =
        <div class="annotations">
            {
                this.annotations(method) map { annotation ⇒
                    <div class="annotation">
                        { Unparsed(annotation.replace("\n", "<br>").replace("\t", "&nbsp;&nbsp;&nbsp;")) }
                    </div>
                }
            }
        </div>

    def dump(
        classFile: Option[ClassFile],
        method: Option[Method],
        code: Code,
        resultHeader: Option[String],
        domain: Domain)(
            operandsArray: TheOperandsArray[domain.Operands],
            localsArray: TheLocalsArray[domain.Locals]): Node = {

        val title =
            classFile.
                map(_.thisType.toJava + method.map("{ "+_.toJava+" }").getOrElse("")).
                orElse(method.map(_.toJava))

        val annotations =
            method.map(annotationsAsXHTML(_)).getOrElse(<div class="annotations"></div>)

        createXHTML(
            title,
            Seq[Node](
                title.map(t => <h1>{ t }</h1>).getOrElse(Text("")),
                annotations,
                scala.xml.Unparsed(resultHeader.getOrElse("")),
                dumpTable(code, domain)(operandsArray, localsArray)
            )
        )
    }

    def writeAndOpenDump(node: Node): Option[File] = {
        import java.awt.Desktop
        import java.io.FileOutputStream

        try {
            if (Desktop.isDesktopSupported) {
                val desktop = Desktop.getDesktop()
                val file = File.createTempFile("OPAL-AI-Dump", ".html")
                val fos = new FileOutputStream(file)
                fos.write(node.toString.getBytes("UTF-8"))
                fos.close()
                desktop.open(file)
                return Some(file)
            }
            println("No desktop support available - cannot open the dump in a browser.")
        } catch {
            case e: Exception ⇒
                println("Opening the AI dump in the OS's default app failed: "+e.getMessage)
        }
        println(node.toString)

        None
    }

    private def styles: String =
        process(this.getClass().getResourceAsStream("dump.head.fragment.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    private def jquery: String =
        process(this.getClass().getResourceAsStream("jquery.js"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    private def colResizable: String =
        process(this.getClass().getResourceAsStream("colResizable-1.3.min.js"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    private def dumpTable(
        code: Code,
        domain: Domain)(
            operandsArray: TheOperandsArray[domain.Operands],
            localsArray: TheLocalsArray[domain.Locals]): Node = {

        val indexedExceptionHandlers = indexExceptionHandlers(code).toSeq.sortWith(_._2 < _._2)
        val exceptionHandlers =
            (
                for ((eh, index) ← indexedExceptionHandlers) yield {
                    "⚡: "+index+" "+eh.catchType.map(_.toJava).getOrElse("<finally>")+
                        " ["+eh.startPC+","+eh.endPC+")"+" => "+eh.handlerPC
                }
            ).map(eh ⇒ <p>{ eh }</p>)

        val ids = new java.util.IdentityHashMap[AnyRef, Integer]
        var nextId = 1
        val idsLookup = (value: AnyRef) ⇒ {
            var id = ids.get(value)
            if (id == null) {
                id = nextId
                nextId += 1
                ids.put(value, id)
            }
            id.intValue()
        }

        <div>
            <table>
                <thead>
                    <tr>
                        <th class="pc">PC</th>
                        <th class="instruction">Instruction</th>
                        <th class="stack">Operand Stack</th>
                        <th class="registers">Registers</th>
                        <th class="properties">Properties</th>
                    </tr>
                </thead>
                <tbody>
                    { dumpInstructions(code, domain)(operandsArray, localsArray)(Some(idsLookup)) }
                </tbody>
            </table>
            { exceptionHandlers }
        </div>
    }

    private def annotations(method: Method): Seq[String] = {
        val annotations =
            method.runtimeVisibleAnnotations ++ method.runtimeInvisibleAnnotations
        annotations.map(_.toJava)
    }

    private def caption(classFile: Option[ClassFile], method: Option[Method]): String = {
        val modifiers = if (method.isDefined && method.get.isStatic) "static " else ""
        val typeName = classFile.map(_.thisType.toJava).getOrElse("")
        val methodName = method.map(m ⇒ m.toJava).getOrElse("&lt; method &gt;")
        modifiers + typeName+"{ "+methodName+" }"
    }

    private def indexExceptionHandlers(code: Code) =
        code.exceptionHandlers.zipWithIndex.toMap

    private def dumpInstructions(
        code: Code,
        domain: Domain)(
            operandsArray: TheOperandsArray[domain.Operands],
            localsArray: TheLocalsArray[domain.Locals])(
                implicit ids: Option[AnyRef ⇒ Int]): Array[Node] = {
        val indexedExceptionHandlers = indexExceptionHandlers(code)
        val joinInstructions = code.joinInstructions
        val instrs = code.instructions.zipWithIndex.zip(operandsArray zip localsArray).filter(_._1._1 ne null)
        for (((instruction, pc), (operands, locals)) ← instrs) yield {
            var exceptionHandlers = code.handlersFor(pc).map(indexedExceptionHandlers(_)).mkString(",")
            if (exceptionHandlers.size > 0) exceptionHandlers = "⚡: "+exceptionHandlers
            dumpInstruction(
                pc, code.lineNumber(pc), instruction, joinInstructions.contains(pc),
                Some(exceptionHandlers),
                domain)(
                    operands, locals)
        }
    }

    def dumpInstruction(
        pc: Int,
        lineNumber: Option[Int],
        instruction: Instruction,
        isJoinInstruction: Boolean,
        exceptionHandlers: Option[String],
        domain: Domain)(
            operands: domain.Operands,
            locals: domain.Locals)(
                implicit ids: Option[AnyRef ⇒ Int]): Node = {
        val pcAsXHTML = Unparsed(
            (if (isJoinInstruction) "⇶ " else "") +
                pc.toString +
                exceptionHandlers.map("<br>"+_).getOrElse("") +
                lineNumber.map("<br><i>l="+_+"</i>").getOrElse(""))

        val properties =
            htmlify(domain.properties(pc, valueToString).getOrElse("<None>"))

        <tr class={ if (operands eq null /*||/&& locals eq null*/ ) "not_evaluated" else "evaluated" }>
            <td class="pc">{ pcAsXHTML }</td>
            <td class="instruction">{ Unparsed(instruction.toString(pc).replace("\n", "<br>")) }</td>
            <td class="stack">{ dumpStack(operands) }</td>
            <td class="locals">{ dumpLocals(locals) }</td>
            <td class="properties">{ properties }</td>
        </tr>
    }

    def htmlify(s: String): Node = {
        scala.xml.Unparsed(
            s.replace("<", "&lt;").
                replace(">", "&gt;").
                replace("\n", "<br>").
                replace("\t", "&nbsp;&nbsp;")
        )
    }

    def dumpStack(
        operands: Operands[_ <: AnyRef])(
            implicit ids: Option[AnyRef ⇒ Int]): Node =
        if (operands eq null)
            <em>Information about operands is not available.</em>
        else {
            <ul class="Stack">
                { operands.map(op ⇒ <li>{ valueToString(op) }</li>) }
            </ul>
        }

    def dumpLocals(
        locals: Locals[_ <: AnyRef /**/ ])(
            implicit ids: Option[AnyRef ⇒ Int]): Node = {

        def mapLocal(local: AnyRef): Node = {
            if (local eq null)
                <span class="unused">{ "UNUSED" }</span>
            else
                <span>{ valueToString(local) }</span>
        }

        if (locals eq null)
            <em>Information about the local variables is not available.</em>
        else {
            <ol start="0" class="registers">
                { locals.map { mapLocal(_) }.map(l ⇒ <li>{ l }</li>).iterator }
            </ol>
        }
    }

    def valueToString(value: AnyRef)(implicit ids: Option[AnyRef ⇒ Int]): String =
        value.toString + ids.map("@"+_.apply(value)).getOrElse("")

    def throwableToXHTML(throwable: Throwable): scala.xml.Node = {
        val node =
            if (throwable.getStackTrace() == null ||
                throwable.getStackTrace().size == 0) {
                <div>{ throwable.getClass().getSimpleName()+" "+throwable.getMessage() }</div>
            } else {
                val stackElements =
                    for { stackElement ← throwable.getStackTrace() } yield {
                        <tr>
                            <td>{ stackElement.getClassName() }</td>
                            <td>{ stackElement.getMethodName() }</td>
                            <td>{ stackElement.getLineNumber() }</td>
                        </tr>
                    }
                val summary = throwable.getClass().getSimpleName()+" "+throwable.getMessage()

                <details>
                    <summary>{ summary }</summary>
                    <table>{ stackElements }</table>
                </details>
            }

        if (throwable.getCause() ne null) {
            val causedBy = throwableToXHTML(throwable.getCause())
            <div style="background-color:yellow">{ node } <p>caused by:</p>{ causedBy }</div>
        } else {
            node
        }
    }

    def evaluatedInstructionsToXHTML(evaluated: List[PC]) = {
        val header = "Evaluated instructions: "+evaluated.filter(_ >= 0).size+"<br>"
        val footer = ""
        val subroutineStart = "<details><summary>Subroutine</summary><div style=\"margin-left:2em;\">"
        val subroutineEnd = "</div></details>"

        var openSubroutines = 0
        val asStrings = evaluated.reverse.map { instruction ⇒
            instruction match {
                case SUBROUTINE_START ⇒
                    openSubroutines += 1
                    subroutineStart
                case SUBROUTINE_END ⇒
                    openSubroutines -= 1
                    subroutineEnd
                case _ ⇒ instruction.toString+" "
            }
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

