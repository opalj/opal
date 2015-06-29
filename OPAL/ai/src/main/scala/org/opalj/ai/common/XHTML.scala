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
package common

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

import org.opalj.io.writeAndOpen
import org.opalj.io.process
import org.opalj.br._
import org.opalj.br.instructions._

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

    import org.opalj.ai.util.XHTML._

    /**
     * Stores the time when the last dump was created.
     *
     * We generate dumps on errors only if the specified time has passed by to avoid that
     * we are drowned in dumps. Often, a single bug causes many dumps to be created.
     */
    private[this] var _lastDump = new java.util.concurrent.atomic.AtomicLong(0l)

    private[this] def lastDump_=(currentTimeMillis: Long): Unit = {
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
                if ((currentTime - this.lastDump) > minimumDumpInterval) {
                    this.lastDump = currentTime
                    val title = Some("Generated due to exception: "+e.getMessage())
                    val dump =
                        XHTML.dump(
                            Some(classFile), Some(method), method.body.get,
                            title,
                            theDomain
                        )(operandsArray, localsArray)
                    writeAndOpen(dump, "StateOfIncompleteAbstractInterpretation", ".html")
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
     * @param f The function that performs the validation of the results.
     */
    def dumpOnFailureDuringValidation[T](
        classFile: Option[ClassFile],
        method: Option[Method],
        code: Code,
        result: AIResult,
        minimumDumpInterval: Long = 500l)(
            f: ⇒ T): T = {
        try {
            if (result.wasAborted) throw new RuntimeException("interpretation aborted")
            f
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case e: Throwable ⇒
                val currentTime = System.currentTimeMillis()
                if ((currentTime - this.lastDump) > minimumDumpInterval) {
                    this.lastDump = currentTime
                    writeAndOpen(
                        dump(
                            classFile.get, method.get,
                            "Dump generated due to exception: "+e.getMessage,
                            result),
                        "AIResult",
                        ".html"
                    )
                } else {
                    Console.err.println("dump suppressed: "+e.getMessage)
                }
                throw e
        }
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

        def methodToString(method: Method): String = {
            if (method.isStatic)
                "static "+method.toJava
            else
                method.toJava
        }

        val title =
            classFile.
                map(_.thisType.toJava + method.map("{ "+methodToString(_)+" }").getOrElse("")).
                orElse(method.map(methodToString(_)))

        val annotations =
            method.map(annotationsAsXHTML(_)).getOrElse(<div class="annotations"></div>)

        createXHTML(
            title,
            Seq[Node](
                title.map(t ⇒ <h1>{ t }</h1>).getOrElse(Text("")),
                annotations,
                scala.xml.Unparsed(resultHeader.getOrElse("")),
                dumpTable(code, domain)(operandsArray, localsArray)
            )
        )
    }

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

        // We cannot create "reasonable output in case of VERY VERY large methods"
        // E.g., a method with 30000 instructions and 1000 locals would create
        // a table with ~ 30.000.000 rows...
        val rowsCount = code.instructionsCount * code.maxLocals
        val operandsOnly = rowsCount > 100000
        val disclaimer =
            if (operandsOnly) {
                <b>Output is restricted to the operands as the number of rows would be too large otherwise: { rowsCount } </b>
            } else
                NodeSeq.Empty

        <div>
            { disclaimer }
            <table>
                <thead>
                    <tr>
                        <th class="pc">PC</th>
                        <th class="instruction">Instruction</th>
                        <th class="stack">Operand Stack</th>
                        {
                            if (operandsOnly) NodeSeq.Empty else {
                                <th class="registers">Registers</th>
                                <th class="properties">Properties</th>
                            }
                        }
                    </tr>
                </thead>
                <tbody>
                    { dumpInstructions(code, domain, operandsOnly)(operandsArray, localsArray)(Some(idsLookup)) }
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

    private def indexExceptionHandlers(code: Code) =
        code.exceptionHandlers.zipWithIndex.toMap

    private def dumpInstructions(
        code: Code,
        domain: Domain,
        operandsOnly: Boolean)(
            operandsArray: TheOperandsArray[domain.Operands],
            localsArray: TheLocalsArray[domain.Locals])(
                implicit ids: Option[AnyRef ⇒ Int]): Array[Node] = {

        val belongsToSubroutine = code.belongsToSubroutine()
        val indexedExceptionHandlers = indexExceptionHandlers(code)
        val joinInstructions = code.joinInstructions
        val instrs = code.instructions.zipWithIndex.zip(operandsArray zip localsArray).filter(_._1._1 ne null)
        for (((instruction, pc), (operands, locals)) ← instrs) yield {
            var exceptionHandlers = code.handlersFor(pc).map(indexedExceptionHandlers(_)).mkString(",")
            if (exceptionHandlers.size > 0) exceptionHandlers = "⚡: "+exceptionHandlers
            dumpInstruction(
                pc, code.lineNumber(pc), instruction, joinInstructions.contains(pc),
                belongsToSubroutine(pc),
                Some(exceptionHandlers),
                domain,
                operandsOnly)(
                    operands, locals)
        }
    }

    def dumpInstruction(
        pc: Int,
        lineNumber: Option[Int],
        instruction: Instruction,
        isJoinInstruction: Boolean,
        subroutineId: Int,
        exceptionHandlers: Option[String],
        domain: Domain,
        operandsOnly: Boolean)(
            operands: domain.Operands,
            locals: domain.Locals)(
                implicit ids: Option[AnyRef ⇒ Int]): Node = {

        val pcAsXHTML =
            Unparsed(
                (if (isJoinInstruction) "⇶ " else "") +
                    pc.toString +
                    exceptionHandlers.map("<br>"+_).getOrElse("") +
                    lineNumber.map("<br><i>l="+_+"</i>").getOrElse("") +
                    (if (subroutineId != 0) "<br><b>⥂="+subroutineId+"</b>" else "")
            )

        val properties =
            htmlify(domain.properties(pc, valueToString).getOrElse("<None>"))

        val instructionAsXHTML =
            // to handle cases where the string contains "executable" (JavaScript) code
            Unparsed(Text(instruction.toString(pc)).toString.replace("\n", "<br>"))

        <tr class={ if (operands eq null /*||/&& locals eq null*/ ) "not_evaluated" else "evaluated" }>
            <td class="pc">{ pcAsXHTML }</td>
            <td class="instruction">{ instructionAsXHTML }</td>
            <td class="stack">{ dumpStack(operands) }</td>
            {
                if (operandsOnly) NodeSeq.Empty else {
                    <td class="locals">{ dumpLocals(locals) }</td>
                    <td class="properties">{ properties }</td>
                }
            }
        </tr>
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

}

