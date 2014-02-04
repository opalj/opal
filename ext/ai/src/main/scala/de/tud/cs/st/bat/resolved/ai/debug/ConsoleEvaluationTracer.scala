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
 * A tracer that prints out the evaluation order on the console.
 *
 * Every AI should have its own instance.
 *
 * @author Michael Eichberg
 */
trait ConsoleEvaluationTracer extends AITracer {

    private[this] var indent = 0

    override def instructionEvalution[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit = {
        print(pc+" ")
    }

    override def continuingInterpretation[D <: SomeDomain with Singleton](
        code: Code,
        domain: D,
        initialWorkList: List[PC],
        alreadyEvaluated: List[PC],
        operandsArray: Array[List[D#DomainValue]],
        localsArray: Array[Array[D#DomainValue]]) { /*EMPTY*/ }

    override def rescheduled[D <: SomeDomain with Singleton](
        domain: D,
        sourcePC: PC,
        targetPC: PC): Unit = { /*EMPTY*/ }

    override def flow[D <: SomeDomain with Singleton](
        domain: D,
        currentPC: PC,
        targetPC: PC): Unit = { /*EMPTY*/ }

    override def join[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals,
        result: Update[(D#Operands, D#Locals)]): Unit = { /*EMPTY*/ }

    override def abruptMethodExecution[D <: SomeDomain with Singleton](
        domain: D,
        pc: Int,
        exception: D#DomainValue): Unit = { /*EMPTY*/ }

    private[this] def printIndent = (0 until indent) foreach (i ⇒ print("\t"))
    
    override def jumpToSubroutine(domain: SomeDomain, pc: PC): Unit = {
        println
        printIndent
        print(Console.BOLD+"↳\t︎"+Console.RESET)
        indent += 1
    }

    override def returnFromSubroutine[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        returnAddress: PC,
        subroutineInstructions: List[PC]): Unit = {
        indent -= 1
        println(Console.BOLD+"✓"+"(Resetting: "+subroutineInstructions.mkString(", ")+")"+Console.RESET)
        printIndent
    }

    /**
     * Called when a ret instruction is encountered.
     */
    override def ret[D <: SomeDomain with Singleton](
        domain: D,
        pc: PC,
        returnAddress: PC,
        oldWorklist: List[PC],
        newWorklist: List[PC]): Unit = { /*EMPTY*/ }

    override def result[D <: SomeDomain with Singleton](result: AIResult[D]) { /*EMPTY*/ }
}
