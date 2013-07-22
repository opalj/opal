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

/**
 * Several utility methods to faciliate the development of the abstract interpreter/
 * new domains for the abstract interpreter.
 *
 * @author Michael Eichberg
 */
object Util {

    import de.tud.cs.st.util.ControlAbstractions._

    def dump(classFile: Option[ClassFile],
             method: Option[Method],
             code: Code,
             memoryLayouts: IndexedSeq[MemoryLayout[_ <: AnyRef, _ <: AnyRef]]): Node = {
        // HTML 5 XML serialization (XHTML 5)
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
        <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8' />
        <style>
        { styles }
        </style>
        </head>
        <body>
        { dumpTable(classFile, method, code, memoryLayouts) }
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

    private lazy val styles: String = {
        withResource(this.getClass().getResourceAsStream("dump.head.fragment.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )
    }

    def dumpTable(classFile: Option[ClassFile],
                  method: Option[Method],
                  code: Code,
                  memoryLayouts: IndexedSeq[MemoryLayout[_ <: AnyRef, _ <: AnyRef]]): Node = {

        <table>
            <caption>{ caption(classFile, method) }</caption>
            <thead>
            <tr><th>PC</th><th>Instruction</th><th>Operand Stack</th><th>Registers</th></tr>
            </thead>
            <tbody>
            { dumpInstructions(code, memoryLayouts) }
            </tbody>
        </table>
    }

    private def caption(classFile: Option[ClassFile],
                        method: Option[Method]): String = {
        method.map(m ⇒ if (m.isStatic) "static " else "").getOrElse("") +
            classFile.map(_.thisClass.toJava+".").getOrElse("") +
            method.map(m ⇒ m.name + m.descriptor.toUMLNotation+" - ").getOrElse("")+
            "Results"
    }

    private def dumpInstructions(code: Code,
                                 memoryLayouts: IndexedSeq[MemoryLayout[_ <: AnyRef, _ <: AnyRef]]) = {
        val instrs = code.instructions.zipWithIndex.zip(memoryLayouts).filter(_._1._1 ne null)
        for (((instruction, pc), memoryLayout) ← instrs) yield {
            <tr class={ if (memoryLayout eq null) "not_evaluated" else "evaluated" }>
              <td>{ pc }</td>
              <td>{ instruction }</td>
              <td>{ dumpStack(memoryLayout) }</td>
              <td>{ dumpLocals(memoryLayout) }</td>
            </tr >
        }
    }

    private def dumpStack(memoryLayout: MemoryLayout[_ <: AnyRef, _ <: AnyRef]) = {
        if (memoryLayout eq null)
            <em>Memory layout not available.</em>
        else {
            <ul style="list-style:none;margin-left:0;padding-left:0">
            { memoryLayout.operands.map(op ⇒ <li>{ op.toString }</li>) }
            </ul>
        }
    }

    private def dumpLocals(memoryLayout: MemoryLayout[_ <: AnyRef, _ <: AnyRef]) = {
        if (memoryLayout eq null)
            <em>Memory layout not available.</em>
        else {
            val locals = memoryLayout.locals
            <ol start="0">
            { locals.map(l ⇒ if (l eq null) "UNUSED" else l.toString()).map(l ⇒ <li>{ l }</li>) }
            </ol>
        }
    }

}
