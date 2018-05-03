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
package org.opalj.support.debug

import org.opalj.collection.immutable.{Chain ⇒ List}
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction
import org.opalj.ai.Domain
import org.opalj.ai.AITracer
import org.opalj.ai.NoUpdate
import org.opalj.ai.Update
import org.opalj.ai.AIResult
import org.opalj.ai.SomeUpdate
import org.opalj.ai.IsReferenceValue
import org.opalj.ai.domain
import org.opalj.ai.domain.TheCode
import org.opalj.collection.mutable.IntArrayStack

/**
 * A tracer that prints out a trace's results on the console.
 *
 * @author Michael Eichberg
 */
trait ConsoleTracer extends AITracer { tracer ⇒

    import Console._

    val printOIDs: Boolean = false

    def oidString(value: Object): String = s"[#${System.identityHashCode(value).toHexString}]"

    def toStringWithOID(value: Object): String = s"$value ${oidString(value)}"

    private def localsToString(domain: Domain)(locals: domain.Locals): String = {
        if (locals eq null)
            "\tlocals: not available (null);\n"
        else
            locals.zipWithIndex.map { vi ⇒
                val (v, i) = vi
                i+":"+(
                    if (v == null)
                        correctIndent("-", false)
                    else
                        correctIndent(v, printOIDs)
                )
            }.mkString("\tlocals:\n\t\t", "\n\t\t", "\n")
    }

    def initialLocals(
        domain: Domain
    )(
        locals: domain.Locals
    ): Unit = {
        println(localsToString(domain)(locals))
    }

    private def correctIndent(value: Object, printOIDs: Boolean): String = {
        if (value eq null)
            return "<EMPTY>";

        def toString(value: Object) =
            value.toString.replaceAll("\n\t", "\n\t\t\t").replaceAll("\n\\)", "\n\t\t)")

        def toStringWithOID(value: Object) = toString(value)+" "+oidString(value)

        if (printOIDs) {
            value match {
                case rv: IsReferenceValue[_] if rv.allValues.size > 1 ⇒
                    val values = rv.allValues
                    val t =
                        if (rv.isInstanceOf[domain.l1.ReferenceValues#ReferenceValue])
                            s";refId=${rv.asInstanceOf[org.opalj.ai.domain.l1.ReferenceValues#ReferenceValue].refId}"
                        else
                            ""
                    values.map(toStringWithOID(_)).mkString("OneOf["+values.size+"](", ",", ")") +
                        rv.upperTypeBound.map(_.toJava).mkString(";lutb=", " with ", ";") +
                        s"isPrecise=${rv.isPrecise};isNull=${rv.isNull}$t "+
                        oidString(rv)
                case _ ⇒
                    toStringWithOID(value)
            }
        } else {
            toString(value)
        }
    }

    private def line(domain: Domain, pc: Int): String = {
        domain match {
            case d: TheCode ⇒ d.code.lineNumber(pc).map("[line="+_+"]").getOrElse("")
            case _          ⇒ ""
        }
    }

    override def instructionEvalution(
        domain: Domain
    )(
        pc:          Int,
        instruction: Instruction,
        operands:    domain.Operands,
        locals:      domain.Locals
    ): Unit = {

        val os =
            if (operands eq null)
                "\toperands: not available (null);\n"
            else {
                val os = operands.map { o ⇒
                    correctIndent(o, printOIDs)
                }
                if (os.isEmpty)
                    "\toperands <NONE>;\n"
                else
                    os.mkString("\toperands:\n\t\t", "\n\t\t", "\n\t;\n")
            }

        val ls = localsToString(domain)(locals)

        val ps = {
            val ps = domain.properties(pc)
            if ((ps eq null) || ps == None)
                ""
            else {
                s"\tproperties: ${ps.get}\n"
            }
        }

        println(
            Console.BLUE + pc + line(domain, pc) + Console.RESET+
                ":" ++ Console.YELLOW_B + instruction.toString(pc) + Console.RESET+
                "; state before execution:[\n"+os + ls + ps+"\t]"
        )
    }

    override def continuingInterpretation(
        code:   Code,
        domain: Domain
    )(
        initialWorkList:                  List[Int /*PC*/ ],
        alreadyEvaluatedPCs:              IntArrayStack,
        operandsArray:                    domain.OperandsArray,
        localsArray:                      domain.LocalsArray,
        memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , domain.OperandsArray, domain.LocalsArray)]
    ): Unit = {

        println(BLACK_B + WHITE+"Starting Code Analysis"+RESET)
        println("Number of registers:      "+code.maxLocals)
        println("Size of operand stack:    "+code.maxStack)
        println("PCs where paths join:     "+code.cfJoins.mkString(", "))
        //println("Program counters:         "+code.programCounters.mkString(", "))
    }

    override def rescheduled(
        domain: Domain
    )(
        sourcePC:                 Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        worklist:                 List[Int /*PC*/ ]
    ): Unit = {
        println(
            CYAN_B + RED + sourcePC + line(domain, sourcePC)+
                ": rescheduled the evaluation of instruction "+
                targetPC + line(domain, targetPC) + RESET+
                " => new worklist: "+worklist.mkString(", ")
        )
    }

    override def flow(
        domain: Domain
    )(
        currentPC:                Int,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean
    ): Unit = { /* ignored */ }

    override def deadLocalVariable(domain: Domain)(pc: Int, lvIndex: Int): Unit = {
        println(
            pc.toString + line(domain, pc).toString+":"+
                Console.BLACK_B + Console.WHITE + s"local variable $lvIndex is dead"
        )
    }

    override def noFlow(domain: Domain)(currentPC: Int, targetPC: Int): Unit = {
        println(Console.RED_B + Console.YELLOW+
            "did not schedule the interpretation of instruction "+
            targetPC + line(domain, targetPC)+
            "; the abstract state didn't change"+Console.RESET)
    }

    override def join(
        domain: Domain
    )(
        pc:            Int,
        thisOperands:  domain.Operands,
        thisLocals:    domain.Locals,
        otherOperands: domain.Operands,
        otherLocals:   domain.Locals,
        result:        Update[(domain.Operands, domain.Locals)]
    ): Unit = {

        print(Console.BLUE + pc + line(domain, pc)+": JOIN: ")
        result match {
            case NoUpdate ⇒ println("no changes")
            case u @ SomeUpdate((updatedOperands, updatedLocals)) ⇒
                println(u.updateType)
                println(
                    thisOperands.
                        zip(otherOperands).
                        zip(updatedOperands).
                        map { v ⇒
                            val ((thisOp, thatOp), updatedOp) = v
                            val s = if (thisOp eq updatedOp)
                                "✓ "+Console.GREEN + updatedOp.toString
                            else {
                                "given "+correctIndent(thisOp, printOIDs)+"\n\t\t join "+
                                    correctIndent(thatOp, printOIDs)+"\n\t\t   => "+
                                    correctIndent(updatedOp, printOIDs)
                            }
                            s + Console.RESET
                        }.
                        mkString("\tOperands:\n\t\t", "\n\t\t", "")
                )
                println(
                    thisLocals.
                        zip(otherLocals).
                        zip(updatedLocals.iterator).map(v ⇒ (v._1._1, v._1._2, v._2)).
                        zipWithIndex.map(v ⇒ (v._2, v._1._1, v._1._2, v._1._3)).
                        filterNot(v ⇒ (v._2 eq null) && (v._3 eq null)).
                        map(v ⇒
                            v._1 + {
                                if (v._2 == v._3)
                                    if (v._2 eq v._3)
                                        Console.GREEN+": ✓ "
                                    else
                                        Console.YELLOW+":(✓) "+
                                            oidString(v._2)+
                                            " join "+
                                            oidString(v._3)+
                                            " => "
                                else {
                                    ":\n\t\t   given "+correctIndent(v._2, printOIDs)+
                                        "\n\t\t    join "+correctIndent(v._3, printOIDs)+
                                        "\n\t\t      => "
                                }
                            } +
                                correctIndent(v._4, printOIDs)).
                        mkString("\tLocals:\n\t\t", "\n\t\t"+Console.BLUE, Console.BLUE)
                )
        }
        println(Console.RESET)
    }

    override def establishedConstraint(
        domain: Domain
    )(
        pc:          Int,
        effectivePC: Int,
        operands:    domain.Operands,
        locals:      domain.Locals,
        newOperands: domain.Operands,
        newLocals:   domain.Locals
    ): Unit = {
        println(
            pc + line(domain, pc)+":"+
                YELLOW_B + BLUE+"Establishing Constraint w.r.t. "+
                effectivePC + line(domain, effectivePC)+":"
        )
        val changedOperands = operands.zip(newOperands).filter(ops ⇒ ops._1 ne ops._2)
        if (changedOperands.nonEmpty) {
            println(YELLOW_B + BLUE+"\tUpdated Operands:")
            changedOperands.foreach(ops ⇒ print(YELLOW_B + BLUE+"\t\t"+ops._1+" => "+ops._2+"\n"))
        }
        val changedLocals =
            locals.zip(newLocals).zipWithIndex.map(localsWithIdx ⇒
                (localsWithIdx._1._1, localsWithIdx._1._2, localsWithIdx._2)).
                filter(ops ⇒ ops._1 ne ops._2)
        if (changedLocals.hasNext) {
            println(YELLOW_B + BLUE+"\tUpdated Locals:")
            changedLocals.foreach(locals ⇒
                print(YELLOW_B + BLUE+"\t\t"+locals._3+":"+
                    toStringWithOID(locals._1)+" => "+
                    toStringWithOID(locals._2)+"\n"))
        }
        println(YELLOW_B + BLUE+"\tDone"+RESET)

    }

    override def abruptMethodExecution(
        domain: Domain
    )(
        pc:        Int,
        exception: domain.ExceptionValue
    ): Unit = {
        println(
            BOLD + RED + pc + line(domain, pc)+
                ":RETURN FROM METHOD DUE TO UNHANDLED EXCEPTION: "+exception +
                RESET
        )
    }

    override def jumpToSubroutine(
        domain: Domain
    )(
        pc: Int, target: Int, nestingLevel: Int
    ): Unit = {
        import Console._
        println(
            pc + line(domain, pc)+":"+YELLOW_B + BOLD+
                "JUMP TO SUBROUTINE(Nesting level: "+nestingLevel+"): "+target +
                RESET
        )
    }

    override def returnFromSubroutine(
        domain: Domain
    )(
        pc:            Int,
        returnAddress: Int,
        subroutinePCs: List[Int /*PC*/ ]
    ): Unit = {
        println(
            YELLOW_B + BOLD + pc + line(domain, pc)+
                ":RETURN FROM SUBROUTINE: target="+returnAddress+
                " : RESETTING : "+subroutinePCs.mkString(", ") +
                RESET
        )
    }

    override def abruptSubroutineTermination(
        domain: Domain
    )(
        details:  String,
        sourcePC: Int, targetPC: Int,
        jumpToSubroutineId:         Int,
        terminatedSubroutinesCount: Int,
        forceScheduling:            Boolean,
        oldWorklist:                List[Int /*PC*/ ],
        newWorklist:                List[Int /*PC*/ ]
    ): Unit = {
        println(
            RED_B + WHITE + sourcePC + line(domain, sourcePC)+
                ":ABRUPT RETURN FROM SUBROUTINE: target="+targetPC +
                s"\n$RED_B$WHITE\t\tdetails                = $details$RESET"+
                "\n"+RED_B + WHITE+"\t\ttarget subroutine id   = "+jumpToSubroutineId + RESET +
                s"\n$RED_B$WHITE\t\t#terminated subroutines= $terminatedSubroutinesCount$RESET"+
                "\n"+RED_B + WHITE+"\t\tforceScheduling        = "+forceScheduling + RESET+
                "\n"+RED_B + WHITE+"\t\told worklist           : "+oldWorklist.mkString(",") + RESET+
                "\n"+RED_B + WHITE+"\t\tnew worklist           : "+newWorklist.mkString(",") + RESET
        )
    }

    /**
     * Called when a ret instruction is encountered.
     */
    override def ret(
        domain: Domain
    )(
        pc:            Int,
        returnAddress: Int,
        oldWorklist:   List[Int /*PC*/ ],
        newWorklist:   List[Int /*PC*/ ]
    ): Unit = {
        println(
            GREEN_B + BOLD + pc + line(domain, pc)+
                ":RET : target="+returnAddress+
                " : OLD_WORKLIST : "+oldWorklist.mkString(", ")+
                " : NEW_WORKLIST : "+newWorklist.mkString(", ") +
                RESET
        )
    }

    override def result(result: AIResult): Unit = { /*ignored*/ }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[Int], message: ⇒ String
    ): Unit = {
        val loc = pc.map(pc ⇒ s"$pc:").getOrElse("<NO PC>")
        println(
            s"$loc[Domain:${source.getSimpleName().split('$')(0)} - $typeID] $message"
        )
    }
}
