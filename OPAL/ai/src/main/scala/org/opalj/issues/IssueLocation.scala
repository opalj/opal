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
package issues

import scala.xml.Node
import scala.xml.Text
import org.opalj.collection.mutable.Locals
import org.opalj.br.PC
import org.opalj.br.instructions.Instruction
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field
import org.opalj.ai.domain.l1.IntegerRangeValues
import scala.xml.Group

sealed trait IssueLocation extends IssueRepresentations {

    /**
     * The description of the issue w.r.t. the location.
     */
    def description: Option[String]

    def compare(other: IssueLocation): Int

}

class ProjectLocation(
        val description: Option[String],
        val theProject:  SomeProject,
        val details:     Seq[IssueDetails] = List.empty
) extends IssueLocation {

    def compare(other: IssueLocation): Int = {
        other match {
            case that: InstructionLocation ⇒ 1
            case that: MethodLocation      ⇒ 1
            case that: FieldLocation       ⇒ 1
            case that: ClassLocation       ⇒ 1
            case that: PackageLocation     ⇒ 1
            case that: ProjectLocation ⇒
                that.theProject.hashCode() compare this.theProject.hashCode() match {
                    case 0 ⇒
                        (this.description, that.description) match {
                            case (None, None)       ⇒ 0
                            case (Some(_), None)    ⇒ -1
                            case (None, Some(_))    ⇒ 1
                            case (Some(x), Some(y)) ⇒ x compare y
                        }
                    case x ⇒ x
                }

        }
    }

    override def toXHTML(basicInfoOnly: Boolean): Node = {
        Group(List(
            <dt>project</dt>,
            <dd>{ description }</dd>
        ))
    }

    override def toAnsiColoredString: String = {
        description.map(_.replace('\n', ';')).getOrElse("")
    }

    override def toEclipseConsoleString: String = {
        description.map(_.replace('\n', ';')).getOrElse("")
    }

    override def toIDL: String = {
        description.map { description ⇒
            "\tproject:\n\t\t"+description.replace("\n", "\n\t\t")+"\n"
        }.getOrElse("")
    }
}

class PackageLocation(
        description:    Option[String],
        theProject:     SomeProject,
        val thePackage: String,
        details:        Seq[IssueDetails] = List.empty
) extends ProjectLocation(description, theProject, details) {

}

class ClassLocation(
        description:   Option[String],
        theProject:    SomeProject,
        val classFile: ClassFile,
        details:       Seq[IssueDetails] = List.empty
) extends PackageLocation(description, theProject, classFile.thisType.packageName, details) {

    override def toAnsiColoredString: String = {
        theProject.source(classFile.thisType).map(_.toString).getOrElse("<No Source>")+":"
    }

    override def toIDL: String = {
        ""+
            "\tPackage: "+thePackage+"\n"+
            "\tClass: "+classFile.thisType.simpleName+"\n"
    }
}

class MethodLocation(
        description: Option[String],
        theProject:  SomeProject,
        classFile:   ClassFile,
        val method:  Method,
        details:     Seq[IssueDetails] = List.empty
) extends ClassLocation(description, theProject, classFile, details) {

}

class InstructionLocation(
    description: Option[String],
    theProject:  SomeProject,
    classFile:   ClassFile,
    method:      Method,
    val pc:      PC,
    details:     Seq[IssueDetails] = List.empty
) extends MethodLocation(description, theProject, classFile, method, details)
        with PCLineConversions {

    assert(method.body.isDefined)

    def code: Code = method.body.get

    override def toEclipseConsoleString: String = {
        val source = classFile.thisType.toJava.split('$').head
        val line = this.line.map(":"+_).getOrElse("")
        "("+source+".java"+line+") "
    }

    override def toAnsiColoredString: String = {
        theProject.source(classFile.thisType).map(_.toString).getOrElse("<No Source>")+":"+
            line.map(_+": ").getOrElse(" ")
    }

}

class FieldLocation(
        description: Option[String],
        theProject:  SomeProject,
        classFile:   ClassFile,
        val field:   Field,
        details:     Seq[IssueDetails] = List.empty
) extends ClassLocation(description, theProject, classFile, details) {

}

