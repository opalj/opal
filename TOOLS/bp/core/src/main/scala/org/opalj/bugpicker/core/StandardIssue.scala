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
package core

import scala.language.existentials
import scala.Console.{GREEN, RESET}
import scala.xml.Node
import scala.xml.Text
import org.opalj.br.PC
import org.opalj.br.methodToXHTML
import org.opalj.br.typeToXHTML
import org.opalj.collection.mutable.Locals
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._
import scala.xml.UnprefixedAttribute
import scala.xml.Unparsed

/**
 * Describes an issue found by an analysis.
 *
 * @param summary A short, one line, single sentence description of the issue.
 * @param description A comprehensive description of the issue potentially along with a
 *                    description how to fix the issue.
 *
 * @author Michael Eichberg
 */
case class StandardIssue(
        analysis:       String,
        project:        SomeProject,
        classFile:      ClassFile,
        method:         Option[Method],
        pc:             Option[PC],
        operands:       Option[List[_ <: AnyRef]],
        localVariables: Option[Locals[_ <: AnyRef]],
        summary:        String,
        description:    Option[String],
        categories:     Set[String],
        kinds:          Set[String],
        otherPCs:       Seq[(PC, String)],
        relevance:      Relevance
) extends Issue {

    /**
     * Merges this issue with the given issue if both issues refer to the same element.
     *
     * @return `Some(StandardIssue)` if this standard issue and the other standard issue
     *      can be merged; `None` otherwise.
     */
    def merge(other: StandardIssue): Option[StandardIssue] = {
        val tMethod = this.method
        val oMethod = other.method

        if ((other.project ne this.project) ||
            (other.analysis != this.analysis) ||
            (other.classFile ne this.classFile) ||
            ((oMethod.isDefined && tMethod.isDefined && (oMethod.get ne tMethod.get)) ||
                (oMethod.isEmpty && tMethod.isDefined) || (oMethod.isDefined && tMethod.isEmpty)) ||
                (other.pc != this.pc))
            return None

        val analysis =
            if (this.analysis.contains(other.analysis))
                this.analysis
            else if (other.analysis.contains(this.analysis))
                other.analysis
            else
                this.analysis+", "+other.analysis

        Some(
            StandardIssue(
                analysis,
                this.project,
                this.classFile,
                this.method,
                this.pc,
                this.operands.orElse(other.operands),
                this.localVariables.orElse(other.localVariables),
                this.summary+"\n"+other.summary,
                {
                    val td = this.description
                    val od = other.description
                    td.map(_ + od.map("\n"+_).getOrElse("")).orElse(od)
                },
                this.categories ++ other.categories,
                this.kinds ++ other.kinds,
                (this.otherPCs.toSet ++ other.otherPCs).toSeq.sortWith {
                    (a, b) ⇒ a._1 < b._1 || (a._1 == b._1 && a._2 < b._2)
                },
                this.relevance merge other.relevance
            )
        )
    }

    def asXHTML(basicInfoOnly: Boolean): Node = {

        val methodId: Option[String] =
            method.map { method ⇒ method.name + method.descriptor.toJVMDescriptor }

        val firstLineOfMethod: Option[String] =
            method.flatMap(_.body.flatMap(_.firstLineNumber.map { ln ⇒
                (if (ln > 2) (ln - 2) else 0).toString
            }))

        def createPCNode(pc: PC): Node = {
            <span data-class={ classFile.fqn } data-method={ methodId.get } data-pc={ pc.toString } data-show="bytecode">
                { pc.toString }
            </span>
        }

        val pcNode: Option[Node] =
            if (methodId.isDefined && pc.isDefined)
                Some(createPCNode(pc.get))
            else
                None

        def createLineNode(pc: PC, line: Int): Node = {
            <span data-class={ classFile.fqn } data-method={ methodId.get } data-line={ line.toString } data-pc={ pc.toString } data-show="sourcecode">
                { line.toString }
            </span>
        }

        val lineNode: Option[Node] =
            if (methodId.isDefined && pc.isDefined && line.isDefined)
                Some(createLineNode(pc.get, line.get))
            else
                None

        val instructionNode: Seq[Node] =
            if (this.instruction.isDefined && this.operands.isDefined && this.localVariables.isDefined) {
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
                            <span class="value">{ operands.head } </span>,
                            <span> (case values: { cbi.caseValues.mkString(", ") } )</span>
                        )

                    case smi: StackManagementInstruction ⇒
                        val representation =
                            <span class="keyword">{ smi.mnemonic } </span> ::
                                operands.map(op ⇒ <span class="value">{ op } </span>)
                        representation

                    case IINC(lvIndex, constValue) ⇒
                        val representation =
                            List(
                                <span class="keyword">iinc </span>,
                                <span class="parameters">
                                    (
                                    <span class="value">{ localVariables.get(lvIndex) }</span>
                                    <span class="value">{ constValue } </span>
                                    )
                                </span>
                            )
                        representation

                    case instruction ⇒
                        val operandsCount =
                            instruction.numberOfPoppedOperands { x ⇒ throw new UnknownError() }

                        val parametersNode =
                            operands.take(operandsCount).reverse.map { op ⇒
                                <span class="value">{ op } </span>
                            }
                        List(
                            <span class="keyword">{ instruction.mnemonic } </span>,
                            <span class="parameters">({ parametersNode })</span>
                        )
                }
            } else
                Seq.empty[Node]

        //
        // BUILDING THE FINAL DOCUMENT
        //

        var infoNodes: List[Node] =
            List(
                <dt class="analysis">analysis</dt>,
                <dd>{ analysis }</dd>,
                <dt>class</dt>,
                <dd class="declaring_class" data-class={ classFile.fqn }>
                    { typeToXHTML(classFile.accessFlags, classFile.thisType, true) }
                </dd>
            )

        if (method.isDefined) {
            val method = this.method.get
            val dt =
                <dt>method</dt>
            var dd =
                <dd class="method" data-class={ classFile.fqn }>
                    { methodToXHTML(method.accessFlags, method.name, method.descriptor, true) }
                </dd>
            if (methodId.isDefined)
                dd = dd % (new UnprefixedAttribute(
                    "data-method",
                    methodId.get.toString,
                    scala.xml.Null
                ))

            if (firstLineOfMethod.isDefined) {
                dd = dd % (new UnprefixedAttribute(
                    "data-line",
                    firstLineOfMethod.get.toString,
                    scala.xml.Null
                ))
            }

            infoNodes = infoNodes ::: List(dt, dd)
        }
        if (pcNode.isDefined || lineNode.isDefined) {
            val dt = <dt>instruction</dt>
            var locations = List.empty[Node]

            // Path information...
            if (!basicInfoOnly) {
                otherPCs.reverse.foreach { info ⇒
                    val (pc, message) = info
                    val lineNode =
                        line(pc).map(ln ⇒
                            <span class="line_number">line={ createLineNode(pc, ln) }</span>).getOrElse(Text(""))

                    locations ::=
                        <div class="issue_additional_info">
                            <span class="program_counter">pc={ createPCNode(pc) }</span>
                            { lineNode }
                            <br/>
                            { message }
                        </div>
                }
            }

            // The primary message...
            locations ::= <br/>
            lineNode.foreach(ln ⇒
                locations =
                    <span class="line_number">line={ lineNode.get }</span> ::
                        locations)
            pcNode.foreach(ln ⇒
                locations =
                    <span class="program_counter">pc={ pcNode.get }</span> ::
                        Text(" ") ::
                        locations)

            val dd =
                <dd> { locations }</dd>
            infoNodes = infoNodes ::: List(dt, dd)
        }
        infoNodes ++= List(
            <dt>summary</dt>,
            <dd>
                <span class="issue_summary">{ Unparsed(summary.replace("\n", "<br>")) }</span>
            </dd>
        )
        infoNodes ++= List(
            <dt>relevance</dt>,
            <dd> { relevance.value.toString + s" (${Relevance.toCategoryName(relevance)})" } </dd>
        )
        val localVariablesAsXHTML = if (basicInfoOnly) None else localVariablesToXHTML
        val summaryNode =
            if (localVariablesAsXHTML.isDefined)
                <dt class="issue">details</dt>
            else
                <dt class="issue">
                    details<abbr class="type object_type" title="Local variable information (debug information) is not available.">&#9888;</abbr>
                </dt>

        val dataKinds =
            kinds.map(_.replace(' ', '_')).mkString(" ")

        val dataCategories =
            categories.map(_.replace(' ', '_')).mkString(" ")

        val node =
            <div class="an_issue" style={ s"color:${relevance.asHTMLColor};" } data-relevance={ relevance.value.toString } data-kind={ dataKinds } data-category={ dataCategories }>
                <dl>
                    { infoNodes }
                    { summaryNode }
                    <dd class="issue_message">
                        { description.getOrElse("") }
                        <p>{ instructionNode }</p>
                        { localVariablesAsXHTML.getOrElse(Text("")) }
                    </dd>
                </dl>
            </div>

        node

    }
}

/**
 * Provides factory and factory related methods for [[StandardIssue]]s.
 *
 * @author Michael Eichberg
 */
object StandardIssue {

    /**
     * Creates a new simple standard issues.
     */
    def apply(
        analysis:  String,
        project:   SomeProject,
        classFile: ClassFile,
        summary:   String
    ): StandardIssue = {
        new StandardIssue(
            analysis,
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
            Relevance.DefaultRelevance
        )
    }

    /**
     * Takes a sequence of [[StandardIssue]]s and merges all those issues that
     * refer to the same element (class file, method, pc).
     *
     * @return The sorted (using [[IssueOrdering]]), list of folded [[StandardIssue]]s.
     */
    def fold(issues: Seq[StandardIssue]): Iterable[StandardIssue] = {
        if (issues.isEmpty || issues.tail.isEmpty)
            return issues;

        val sortedIssues = issues.sorted(IssueOrdering)
        val foldedIssues =
            sortedIssues.tail.foldLeft(List(sortedIssues.head)) { (issues, nextIssue) ⇒
                (issues.head merge nextIssue) match {
                    case Some(newIssue) ⇒ newIssue :: issues.tail
                    case None           ⇒ nextIssue :: issues
                }
            }
        foldedIssues
    }
}
