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

import scala.Console.{ GREEN, RESET }
import scala.xml.Node
import scala.xml.Text

import org.opalj.br.methodToXHTML
import org.opalj.br.typeToXHTML
import org.opalj.collection.mutable.Locals
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._

/**
 * Describes some issue found in the source code.
 *
 * @author Michael Eichberg
 */
case class StandardIssue(
        project: SomeProject,
        classFile: ClassFile,
        method: Option[Method],
        pc: Option[PC],
        operands: Option[List[_ <: AnyRef]],
        localVariables: Option[Locals[_ <: AnyRef]],
        summary: String,
        description: Option[String],
        categories: Set[String],
        kind: Set[String],
        otherPCs: Seq[(PC, String)],
        relevance: Relevance) extends Issue {

    def asAnsiColoredString: String = {
        project.source(classFile.thisType).map(_.toString).getOrElse("<No Source>")+":"+
            line.map(_+":").getOrElse("") +
            relevance.asAnsiColoredString + (": ") +
            GREEN + summary + RESET
    }

    def asXHTML: Node = {

        val methodId: Option[String] =
            method.map { method ⇒ method.name + method.descriptor.toJVMDescriptor }

        val firstLineOfMethod: Option[String] =
            method.flatMap(_.body.flatMap(_.firstLineNumber.map { ln ⇒
                (if (ln > 0) (ln - 1) else 0).toString
            }))

        val pcNode: Option[Node] =
            if (methodId.isDefined && pc.isDefined)
                Some(
                    <span data-class={ classFile.fqn } data-method={ methodId.get } data-pc={ pc.get.toString }  data-show="bytecode">
                { pc.get.toString }
            </span>
                )
            else
                None

        val lineNode: Option[Node] =
            if (methodId.isDefined && pc.isDefined && line.isDefined)
                Some(
                    <span data-class={ classFile.fqn } data-method={ methodId.get } data-line={ line.get.toString } data-pc={ pc.get.toString } data-show="sourcecode">
                    { line.get.toString }
                    </span>
                )
            else
                None

        val instructionNode =
            if (this.instruction.isDefined && this.operands.isDefined) {
                val operands = this.operands.get
                this.instruction.get match {
                    case cbi: SimpleConditionalBranchInstruction ⇒

                        val condition =
                            if (cbi.operandCount == 1)
                                List(
                                    <span class="value">{ operands.head } </span>,
                                    <span class="operator">{ cbi.operator } </span>
                                )
                            else
                                List(
                                    <span class="value">{ operands.tail.head } </span>,
                                    <span class="operator">{ cbi.operator } </span>,
                                    <span class="value">{ operands.head } </span>
                                )
                        <span class="keyword">if&nbsp;</span> :: condition

                    case cbi: CompoundConditionalBranchInstruction ⇒
                        Seq(
                            <span class="keyword">switch </span>,
                            <span class="value">{ operands.head } </span>
                        <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                        )
                    case smi: StackManagementInstruction ⇒
                        val representation =
                            <span class="keyword">{ smi.mnemonic } </span> ::
                                operands.map(op ⇒ <span class="value">{ op } </span>)
                        representation

                    case instruction ⇒
                        val operandsCount =
                            instruction.numberOfPoppedOperands { x ⇒ throw new UnknownError() }

                        <span class="keyword">{ instruction.mnemonic } </span> ::
                            Text("(") ::
                            operands.take(operandsCount).reverse.map { op ⇒
                                <span class="value">{ op } </span>
                            } :::
                            List(Text(")"))
                }
            } else
                Seq(Text(""))

        val node =
            <div class="an_issue" style={ s"color:${ relevance.asHTMLColor };" } data-relevance={ relevance.toString }>
                <dl>
                    <dt>class</dt>
                    <dd class="declaring_class" data-class={ classFile.fqn }>{ typeToXHTML(classFile.thisType) }</dd>
                    <dt>method</dt>
                    <dd class="method" data-class={ classFile.fqn } data-method={ methodId } data-line={ methodLine }>
                        { methodToXHTML(method.name, method.descriptor) }
                    </dd>
                    <dt>instruction</dt>
                    <dd>
                        <span class="program_counter">pc={ pcNode }</span>
                        &nbsp;
                        <span class="line_number">line={ lineNode }</span>
                    </dd>
                    <dt class="issue">summary</dt>
                    <dd class="issue_message">
                        { description }
                        <p>{ instructionNode }</p>
                        { localVariablesToXHTML }
                    </dd>
                </dl>
            </div>

        node

    }

}

object StandardIssue {

    def apply(project: SomeProject, classFile: ClassFile, summary: String) = {
        new StandardIssue(
            project,
            classFile,
            None,
            None,
            None,
            None,
            summary,
            None,
            Set.empty,
            Set.empty,
            Seq.empty,
            Relevance.DefaultRelevance)
    }
}
