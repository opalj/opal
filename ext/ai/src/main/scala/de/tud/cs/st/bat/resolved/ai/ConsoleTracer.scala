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

/**
 * A tracer that prints out a trace's results on the console.
 *
 * @author Michael Eichberg
 */
trait ConsoleTracer extends AITracer {

    private def correctIndent(value: Object): String = {
        if (value eq null)
            "<EMPTY>"
        else
            value.toString().replaceAll("\n\t", "\n\t\t\t").replaceAll("\n\\)", "\n\t\t)")
    }

    def instructionEvalution[D <: Domain[_]](
        domain: D,
        pc: PC,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit = {

        println(
            pc+":"+instruction.toString(pc)+" [\n"+
                operands.map { o ⇒
                    correctIndent(o)
                }.mkString("\toperands:\n\t\t", "\n\t\t", "\n\t;\n") + locals.map { l ⇒
                    if (l eq null) "-" else l.toString
                }.zipWithIndex.map { v ⇒
                    v._2+":"+correctIndent(v._1)
                }.mkString("\tlocals:\n\t\t", "\n\t\t", "\n")+"\t]")
    }

    def merge[D <: Domain[_]](
        domain: D,
        pc: PC,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)],
        forcedContinuation: Boolean): Unit = {

        print(Console.BLUE + pc+": MERGE :")
        result match {
            case NoUpdate ⇒ println("no changes; forced continuation="+forcedContinuation)
            case u @ SomeUpdate((updatedOperands, updatedLocals)) ⇒
                println(u.updateType+"; forced continuation="+forcedContinuation)
                println(
                    thisOperands.
                        zip(otherOperands).
                        map(v ⇒ "given "+correctIndent(v._1)+"\n\t\tmerge "+correctIndent(v._2)).
                        zip(updatedOperands).
                        map(v ⇒ v._1+"\n\t\t=>    "+correctIndent(v._2)).
                        mkString("\tOperands:\n\t\t", "\n\t\t----------------\n\t\t", "")
                )
                println(
                    thisLocals.
                        zip(otherLocals).
                        zip(updatedLocals).map(v ⇒ (v._1._1, v._1._2, v._2)).
                        zipWithIndex.map(v ⇒ (v._2, v._1._1, v._1._2, v._1._3)).
                        filterNot(v ⇒ (v._2 eq null) && (v._3 eq null)).
                        map(v ⇒
                            v._1 + {
                                if (v._2 == v._3)
                                    Console.GREEN+": ✓ "
                                else {
                                    Console.MAGENTA+":\n\t\tgiven "+correctIndent(v._2)+
                                        "\n\t\tmerge "+correctIndent(v._3)+
                                        "\n\t\t=>    "
                                }
                            } +
                                correctIndent(v._4)
                        ).
                        mkString("\tLocals:\n\t\t", "\n\t\t"+Console.BLUE, Console.BLUE)
                )
        }
        println(Console.RESET)
    }

    def abruptMethodExecution[D <: Domain[_]](
        domain: D,
        pc: Int,
        exception: D#DomainValue): Unit = {
        println(Console.BOLD +
            Console.RED +
            pc+": RETURN FROM METHOD DUE TO UNHANDLED EXCEPTION :"+exception +
            Console.RESET)
    }

    def returnFromSubroutine[D <: Domain[_]](
        domain: D,
        pc: Int,
        returnAddress: Int,
        subroutineInstructions: List[Int]): Unit = {
        println(Console.YELLOW_B +
            Console.BOLD +
            pc+": RETURN FROM SUBROUTINE : "+returnAddress+
            " : RESETTING : "+subroutineInstructions.mkString(", ") +
            Console.RESET)
    }
}
