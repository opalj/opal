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
package debug

import instructions.Instruction

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

    override def instructionEvalution[D <: SomeDomain with Singleton](
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

    override def continuingInterpretation[D <: SomeDomain with Singleton](
        code: Code,
        domain: D,
        initialWorkList: List[PC],
        alreadyEvaluated: List[PC],
        operandsArray: Array[List[D#DomainValue]],
        localsArray: Array[Array[D#DomainValue]]) {
        println(Console.BLACK_B + Console.WHITE+"Starting Code Analysis"+Console.RESET)
        println("Number of registers:      "+code.maxLocals)
        println("Size of operand stack:    "+code.maxStack)
        //println("Program counters:         "+code.programCounters.mkString(", "))     
    }

    override def rescheduled[D <: SomeDomain with Singleton](
        domain: D,
        sourcePC: PC,
        targetPC: PC): Unit = {
        println(
            Console.CYAN_B + Console.RED+
                "rescheduled the evaluation of the instruction with the program counter: "+
                targetPC +
                Console.RESET)
    }

    override def flow[D <: SomeDomain with Singleton](
        domain: D,
        currentPC: PC,
        targetPC: PC) { /* ignored */ }

    override def join[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)]): Unit = {

        print(Console.BLUE + pc+": MERGE: ")
        result match {
            case NoUpdate ⇒ println("no changes")
            case u @ SomeUpdate((updatedOperands, updatedLocals)) ⇒
                println(u.updateType)
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

    override def abruptMethodExecution[D <: SomeDomain with Singleton](
        domain: D,
        pc: Int,
        exception: D#DomainValue): Unit = {
        println(Console.BOLD +
            Console.RED +
            pc+": RETURN FROM METHOD DUE TO UNHANDLED EXCEPTION :"+exception +
            Console.RESET)
    }

    override def jumpToSubroutine(domain: SomeDomain, pc: PC): Unit = {
        import Console._
        println(YELLOW_B + BOLD+"JUMP TO SUBROUTINE : "+pc + RESET)
    }

    override def returnFromSubroutine[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        returnAddress: PC,
        subroutineInstructions: List[PC]): Unit = {
        println(Console.YELLOW_B +
            Console.BOLD +
            pc+": RETURN FROM SUBROUTINE : "+returnAddress+
            " : RESETTING : "+subroutineInstructions.mkString(", ") +
            Console.RESET)
    }

    /**
     * Called when a ret instruction is encountered.
     */
    override def ret[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        returnAddress: PC,
        oldWorklist: List[PC],
        newWorklist: List[PC]): Unit = {
        println(Console.GREEN_B +
            Console.BOLD +
            pc+": RET : "+returnAddress+
            " : OLD_WORKLIST : "+oldWorklist.mkString(", ")+
            " : NEW_WORKLIST : "+newWorklist.mkString(", ") +
            Console.RESET)
    }

    override def result[D <: SomeDomain with Singleton](result: AIResult[D]) { /*ignored*/ }
}
