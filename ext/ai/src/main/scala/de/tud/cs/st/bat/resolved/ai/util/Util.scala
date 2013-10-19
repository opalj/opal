/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package util

import scala.xml.Node
import java.io.File
import scala.util.control.ControlThrowable

/**
 * Several utility methods to facilitate the development of the abstract interpreter/
 * new domains for the abstract interpreter.
 *
 * @author Michael Eichberg
 */
object Util {

    import language.existentials
    import de.tud.cs.st.util.ControlAbstractions._

    /**
     * We generate dumps on errors only if the specified time has passed (in general
     * a bug causes many dumps to be created.)
     *
     * If you want to generate more dumps set this value to a small(er) value or to -1l if
     * do never want to miss a dump. The default is 2500 (milliseconds).
     */
    @volatile
    var timeInMillisBetweenDumps: Long = 2500l
    private var lastDump: Long = 0l

    def dumpOnFailure[T, D <: SomeDomain](
        classFile: ClassFile,
        method: Method,
        ai: AI[_ >: D],
        domain: D)(
            f: AIResult[domain.type] ⇒ T): T = {
        val result = ai(classFile, method, domain)
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
                if ((currentTime - lastDump) > timeInMillisBetweenDumps) {
                    lastDump = currentTime
                    val title = Some("Generated due to exception: "+e.getMessage())
                    val dump =
                        util.Util.dump(
                            Some(classFile),
                            Some(method),
                            method.body.get,
                            domain,
                            operandsArray,
                            localsArray,
                            title)
                    util.Util.writeAndOpenDump(dump) //.map(_.deleteOnExit)
                } else {
                    Console.err.println("Dump suppressed: "+e.getMessage())
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
     */
    def dumpOnFailureDuringValidation[T](
        classFile: Option[ClassFile],
        method: Option[Method],
        code: Code,
        result: AIResult[_ <: Domain[_]])(
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
                if ((currentTime - lastDump) > timeInMillisBetweenDumps) {
                    lastDump = currentTime
                    writeAndOpenDump(
                        dump(classFile,
                            method,
                            code,
                            result.domain,
                            operandsArray, localsArray,
                            Some("Dump generated due to exception: "+e.getMessage()))
                    )
                } else {
                    Console.err.println("dump suppressed: "+e.getMessage())
                }
                throw e
        }
    }

    def dump(classFile: Option[ClassFile],
             method: Option[Method],
             code: Code,
             domain: SomeDomain,
             operandsArray: Array[_ <: List[_ <: AnyRef]],
             localsArray: Array[_ <: Array[_ <: AnyRef]],
             title: Option[String] = None): Node = {
        // HTML 5 XML serialization (XHTML 5)
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
        <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8' />
        <style>
        { styles }
        </style>
        </head>
        <body>
        { scala.xml.Unparsed(title.getOrElse("")) }
        { dumpTable(classFile, method, code, domain, operandsArray, localsArray) }
        </body>
        </html>
    }

    def writeAndOpenDump(node: Node): Option[File] = {
        import java.awt.Desktop
        import java.io.FileOutputStream

        try {
            if (Desktop.isDesktopSupported) {
                val desktop = Desktop.getDesktop()
                val file = File.createTempFile("BATAI-Dump", ".html")
                val fos = new FileOutputStream(file)
                fos.write(node.toString.getBytes("UTF-8"))
                fos.close()
                desktop.open(file)
                return Some(file)
            }
            println("No desktop support existing - cannot open the dump in a browser.")
        } catch {
            case e: Exception ⇒
                println("Opening the AI dump in the OS's default app failed: "+e.getMessage)
        }
        println(node.toString)

        None
    }

    private lazy val styles: String =
        process(this.getClass().getResourceAsStream("dump.head.fragment.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )

    def dumpTable(
        classFile: Option[ClassFile],
        method: Option[Method],
        code: Code,
        domain: SomeDomain,
        operandsArray: Array[_ <: List[_ <: AnyRef]],
        localsArray: Array[_ <: Array[_ <: AnyRef]]): Node = {

        val indexedExceptionHandlers = indexExceptionHandlers(code)
        <div>
        <table>
            <caption>{ caption(classFile, method) }</caption>
            <thead>
            <tr><th class="pc">PC</th><th class="instruction">Instruction</th><th class="stack">Operand Stack</th><th class="registers">Registers</th><th class="properties">Properties</th></tr>
            </thead>
            <tbody>
            { dumpInstructions(code, domain, operandsArray, localsArray) }
            </tbody>
        </table>
        { for ((eh, index) ← indexedExceptionHandlers) yield <p>
                   { "⚡: " + index + " " + eh.catchType.map(_.toJava).getOrElse("<finally>") + " [" + eh.startPC + "," + eh.endPC + ")" + " => " + eh.handlerPC }
           </p> }
        </div>
    }

    private def caption(
        classFile: Option[ClassFile],
        method: Option[Method]): String = {
        method.map(m ⇒ if (m.isStatic) "static " else "").getOrElse("") +
            classFile.map(_.thisClass.toJava+".").getOrElse("") +
            method.map(m ⇒ m.name + m.descriptor.toUMLNotation+" - ").getOrElse("")+
            "Results"
    }

    private def indexExceptionHandlers(code: Code) =
        Map() ++ code.exceptionHandlers.zipWithIndex

    private def dumpInstructions(
        code: Code,
        domain: SomeDomain,
        operandsArray: Array[_ <: List[_ <: AnyRef]],
        localsArray: Array[_ <: Array[_ <: AnyRef]]) = {
        val indexedExceptionHandlers = indexExceptionHandlers(code)
        val instrs = code.instructions.zipWithIndex.zip(operandsArray zip localsArray).filter(_._1._1 ne null)
        for (((instruction, pc), (operands, locals)) ← instrs) yield {
            var exceptionHandlers = code.exceptionHandlersFor(pc).map(indexedExceptionHandlers(_)).mkString(",")
            if (exceptionHandlers.size > 0) exceptionHandlers = "⚡: "+exceptionHandlers
            dumpInstruction(pc, instruction, domain, operands, locals, Some(exceptionHandlers))
        }
    }

    def dumpInstruction(
        pc: Int,
        instruction: Instruction,
        domain: SomeDomain,
        operands: List[_ <: AnyRef],
        locals: Array[_ <: AnyRef],
        exceptionHandlers: Option[String]) = {
        <tr class={ if (operands eq null /*||/&& locals eq null*/ ) "not_evaluated" else "evaluated" }>
            <td class="pc">{ scala.xml.Unparsed(pc.toString + "<br>" + exceptionHandlers.getOrElse("")) }</td>
            <td class="instruction">{ scala.xml.Unparsed(scala.xml.Text(instruction.toString(pc)).toString.replace("\n", "<br>")) }</td>
            <td class="stack">{ dumpStack(operands) }</td>
            <td class="locals">{ dumpLocals(locals) }</td>
            <td class="properties">{ domain.properties(pc).getOrElse("<None>") }</td>
        </tr >
    }

    type SomeOperands = List[_ <: AnyRef]
    type SomeLocals = Array[_ <: AnyRef]

    def dumpStack(operands: List[_ <: AnyRef]) =
        if (operands eq null)
            <em>Operands are not available.</em>
        else {
            <ul class="Stack">
            { operands.map(op ⇒ <li>{ op.toString }</li>) }
            </ul>
        }

    def dumpLocals(locals: SomeLocals) =
        if (locals eq null)
            <em>Local variables assignment is not available.</em>
        else {
            <ol start="0" class="registers">
            { locals.map(l ⇒ if (l eq null) "UNUSED" else l.toString()).map(l ⇒ <li>{ l }</li>) }
            </ol>
        }

}
