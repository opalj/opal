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
package analysis

import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.xml.Node
import scala.xml.Text
import scala.xml.UnprefixedAttribute

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.methodToXHTML
import org.opalj.br.typeToXHTML
import org.opalj.collection.mutable.Locals

/**
 * Describes a useless computation.
 *
 * @author Michael Eichberg
 */
case class UselessComputation(
        classFile: ClassFile,
        method: Method,
        pc: PC,
        localVariables: Option[Locals[_ <: AnyRef]],
        computation: String) extends Issue {

    override def category: String = IssueCategory.Performance

    override def kind: String = IssueKind.ConstantComputation

    def message: String = {

        val line = this.line.map("(line:"+_+")").getOrElse("")

        "useless computation "+
            classFile.thisType.toJava+"{ "+method.toJava+"{ "+pc + line+": "+computation+" }}"
    }

    override def toString = {
        import Console._

        val line = this.line.map("(line:"+_+")").getOrElse("")

        "useless computation "+
            BOLD + BLUE + classFile.thisType.toJava + RESET+
            "{ "+method.toJava+"{ "+
            GREEN + pc + line+": "+message +
            RESET+" }}"
    }

    def toXHTML: Node = {

        val methodId = method.name + method.descriptor.toJVMDescriptor

        val methodLine: String =
            method.body.flatMap(_.firstLineNumber.map { ln ⇒
                if (ln > 0) (ln - 1).toString else "0"
            }).getOrElse("")

        val pcNode =
            <span data-class={ classFile.fqn } data-method={ methodId } data-line={ line.map(_.toString).getOrElse("") } data-pc={ pc.toString } data-show="bytecode">{ pc }</span>

        val lineNode =
            line.map { ln ⇒
                <span data-class={ classFile.fqn } data-method={ methodId } data-line={ ln.toString } data-pc={ pc.toString } data-show="sourcecode">{ ln }</span>
            }.getOrElse(Text("N/A"))

        val styleAttribute = "color:rgb(126, 64, 64)";

        val classAttribute = "an_issue "+kind

        val node =
            <div class={ classAttribute } style={ styleAttribute }>
                <dl>
                    <dt>class</dt>
                    <dd class="declaring_class" data-class={ classFile.fqn }>{ typeToXHTML(classFile.thisType) }</dd>
                    <dt>method</dt>
                    <dd class="method" data-class={ classFile.fqn } data-method={ methodId } data-line={ methodLine }>
                        { methodToXHTML(method.name, method.descriptor) }
                    </dd>
                    <dt>location</dt>
                    <dd>
                        <span class="program_counter">pc={ pcNode }</span>
                        &nbsp;
                        <span class="line_number">line={ lineNode }</span>
                    </dd>
                    <dt class="issue">issue</dt>
                    <dd class="issue_message">{ computation }</dd>
                </dl>
            </div>

        node % (
            new UnprefixedAttribute("data-relevance", "50", scala.xml.Null)
        )
    }
}

