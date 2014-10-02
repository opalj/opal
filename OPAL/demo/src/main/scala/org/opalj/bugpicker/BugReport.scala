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

import scala.xml.Node
import scala.xml.UnprefixedAttribute
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.collection.SortedMap
import org.opalj.br.{ ClassFile, Method }
import org.opalj.ai.debug.XHTML
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import scala.xml.Unparsed

trait BugReport {

    /**
     * The primarily affected class file.
     */
    def classFile: ClassFile

    /**
     * The primarily affected method.
     */
    def method: Method

    /**
     * The primarily affected instruction.
     */
    def pc: PC

    /**
     * The primarily affected line of source code; if available.
     */
    def line: Option[Int]

    /**
     * A textual representation of the bug report, well suited for console output.
     */
    def message: String

    /**
     * An HTML representation of the bug report, well suited for browser output.
     *
     * The format has to be:
     * {{{
     * &lt;tr style={
     *      val color = accuracy.map(a ⇒ a.asHTMLColor).getOrElse("rgb(255, 126, 3)")
     *      s"color:$color;"
     *      }&gt;
     *  &lt;td&gt;
     *      XHTML.typeToXHTML(classFile.thisType)
     *  &lt;/td&gt;
     *  &lt;td&gt;
     *      XHTML.methodToXHTML(method.name, method.descriptor)
     *  &lt;/td&gt;
     *  &lt;td&gt;
     *      PROGRAM_COUNTER "/" LINE_NUMBER OR "N/A"
     *  &lt;td&gt;
     *      MESSAGE (FREE FORM)
     * &lt;/tr&gt;
     * }}}
     */
    def toXHTML: Node
}

