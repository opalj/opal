/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
import scala.xml.Group

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction

trait ClassComprehension {

    def classFile: ClassFile

    def classFileFQN: String = classFile.fqn

}

trait MethodComprehension extends ClassComprehension {

    def method: Method

    def methodJVMSignature: String = method.name + method.descriptor.toJVMDescriptor

}

trait CodeComprehension {

    implicit def code: Code

    def pc: PC

    final def opcode: Int = code.instructions(pc).opcode

    final def instruction: Instruction = code.instructions(pc)
}

trait PCLineComprehension extends MethodComprehension with CodeComprehension {

    final def line(pc: PC): Option[Int] = PCLineComprehension.line(pc)

    final def line: Option[Int] = line(pc)

    final def pcLineToString = PCLineComprehension.pcLineToString(pc)

    def pcNode: Node = PCLineComprehension.pcNode(classFileFQN, methodJVMSignature, pc)

    def lineNode: Node = PCLineComprehension.lineNode(classFileFQN, methodJVMSignature, pc, line)

}

object PCLineComprehension {

    final def line(pc: PC)(implicit code: Code): Option[Int] = code.lineNumber(pc)

    final def pcLineToString(
        pc: PC
    )(
        implicit
        code: Code
    ) = "pc="+pc + line(pc).map(" line="+_).getOrElse("")

    def pcNode(classFileFQN: String, methodJVMSignature: String, pc: PC): Node = {
        <span class="program_counter" data-class={ classFileFQN } data-method={ methodJVMSignature } data-pc={ pc.toString } data-show="bytecode">
            pc={ pc.toString }
        </span>
    }

    def lineNode(
        classFileFQN:       String,
        methodJVMSignature: String,
        pc:                 PC,
        line:               Option[Int]
    ): Node = {
        line.map { line ⇒
            <span class="line_number" data-class={ classFileFQN } data-method={ methodJVMSignature } data-line={ line.toString } data-pc={ pc.toString } data-show="sourcecode">
                line={ line.toString }
            </span>
        }.getOrElse(Group(Nil))
    }
}

/**
 * Information that facilitates the comprehension of a reported issue.
 */
trait IssueDetails extends IssueRepresentations
