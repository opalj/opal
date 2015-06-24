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
package ai
package domain

import scala.xml.Node
import scala.util.control.ControlThrowable
import scala.collection.BitSet
import org.opalj.br.instructions._
import org.opalj.ai.util.containsInPrefix
import org.opalj.br.Code
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.collection.mutable.ArrayMap
import org.opalj.collection.mutable.{ Locals ⇒ Registers }
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.ComputationalTypeCategory
import org.opalj.ai.util.XHTML
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.DefaultMutableNode

/**
 * Collects the Definition-Use information. I.e., makes the information available
 * which variable is accessed where. Here, all variables are identified using int values.
 * The parameters have negative int values.
 *
 * This domain can be reused to successively perform abstract interpretation of different
 * methods. The domain's initProperties method resets the entire state related to
 * the abstract interpretation of a method.
 *
 * @author Michael Eichberg
 */
trait RecordDefUse extends CoreDomainFunctionality with CustomInitialization {
    domain: Domain ⇒

    type PCs = UShortSet

    // IDEA:
    // EACH LOCAL VARIABLE IS NAMED USING THE PC OF THE INSTRUCTION THAT INITIALIZES IT.
    //
    // EXAMPLE
    // PC:                0:        1:          2:           3:        4:         5:
    // INSTRUCTION        a_load0   getfield    invokestatic a_store0  getstatic  return
    // STACK              empty     [ -1 ]      [ 1 ]        [ 2 ]     empty      [ 4 ]
    // REGISTERS          0: -1     0: -1       0: -1        0: -1     0: 2       0: 1
    // USED BY  "-1":{1}  "0": N/A  "1":{2}     "2":{3}      "3": N/A  "4": {5}   "5": N/A

    private[this] var instructions: Array[Instruction] = _

    // Stores the information where the value defined by the current instruction is
    // used.
    private[this] var used: Array[PCs] = _
    private[this] var parametersOffset: Int = _

    // This array contains the information where each operand value was defined.
    private[this] var defOps: Array[List[Set[ValueOrigin]]] = _
    // This array contains the information where each local is defined;
    // negative values indicate that the values are parameters.
    private[this] var defLocals: Array[Registers[Set[ValueOrigin]]] = _

    abstract override def initProperties(
        code: Code,
        joinInstructions: BitSet,
        locals: Locals): Unit = {

        instructions = code.instructions
        val codeSize = instructions.size

        defOps = new Array(codeSize)
        defOps(0) = Nil

        // initialize initial def-use information based on the parameters
        defLocals = new Array(codeSize)
        var parametersOffset = 0
        var parameterIndex = -1
        defLocals(0) =
            locals.map { v ⇒
                if (v ne null) {
                    parametersOffset += 1
                    val localVar = Set(parameterIndex)
                    parameterIndex -= 1
                    localVar
                } else {
                    null
                }
            }
        this.parametersOffset = parametersOffset

        used = new Array(codeSize + parametersOffset)

        super.initProperties(code, joinInstructions, locals)
    }

    abstract override def properties(
        pc: Int,
        propertyToString: AnyRef ⇒ String): Option[String] = {

        val thisProperty =
            Option(used(pc + parametersOffset)).
                map(_.mkString("UsedBy={", ",", "}"))

        super.properties(pc, propertyToString) match {
            case superProperty @ Some(description) ⇒
                thisProperty map (_+"; "+description) orElse superProperty
            case None ⇒
                thisProperty
        }
    }

    def dumpDefUseInfo(): Node = {
        val perInstruction =
            defOps.zip(defLocals).zipWithIndex.
                filter(e ⇒ (e._1._1 != null || e._1._2 != null)).
                map { e ⇒
                    val ((os, ls), i) = e
                    val operands =
                        if (os eq null)
                            <i>{ "N/A" }</i>
                        else
                            os map { o ⇒
                                <li>{ if (o eq null) "N/A" else o.mkString("{", ",", "}") }</li>
                            }

                    val locals =
                        if (ls eq null)
                            <i>{ "N/A" }</i>
                        else
                            ls.toSeq.reverse.map { e ⇒
                                <li>{ if (e eq null) "N/A" else e.mkString("{", ",", "}") }</li>
                            }

                    val used = this.used(i + parametersOffset)
                    val usedBy = if (used eq null) "N/A" else used.mkString("{", ", ", "}")
                    <tr>
                        <td>{ i }</td>
                        <td>{ usedBy }</td>
                        <td><ul class="Stack">{ operands }</ul></td>
                        <td><ol start="0" class="registers">{ locals }</ol></td>
                    </tr>
                }

        XHTML.createXHTML(
            Some("Definition/Use Information"),
            <table>
                <tr>
                    <th class="pc">PC</th>
                    <th class="pc">Used By</th>
                    <th class="stack">Stack</th>
                    <th class="registers">Locals</th>
                </tr>
                { perInstruction }
            </table>
        )
    }

    def dumpDefUseGraph(code: Code): Set[DefaultMutableNode[ValueOrigin]] = {

        // 1. create set of all def sites
        var defSites: Set[ValueOrigin] = Set.empty
        defOps.filter(_ ne null).foreach { _.foreach { _.foreach { defSites += _ } } }
        for {
            defLocalsPerPC ← this.defLocals
            if defLocalsPerPC ne null
            defLocalsPerPCPerRegister ← defLocalsPerPC.toSeq
            if defLocalsPerPCPerRegister ne null
            valueOrigin ← defLocalsPerPCPerRegister
        } {
            defSites += valueOrigin
        }

        println("Definition sites:"+defSites)

        def instructionToString(vo: ValueOrigin): String = {
            if (vo < 0)
                s"<parameter:${-vo - 1}>"
            else
                s"$vo: "+code.instructions(vo).toString(vo)
        }

        val unusedNode =
            new DefaultMutableNode(
                Int.MinValue: ValueOrigin,
                (vo: ValueOrigin) ⇒ "<NONE>",
                Some("orange")
            )

        // 1. create nodes for all local vars (i.e., the corresponding instructions)
        var nodes: Map[ValueOrigin, DefaultMutableNode[ValueOrigin]] =
            (defSites map { defSite ⇒
                val color =
                    if (defSite < 0)
                        Some("green")
                    else if (code.exceptionHandlers.exists { _.handlerPC == defSite })
                        Some("yellow")
                    else
                        None
                (
                    defSite,
                    new DefaultMutableNode[ValueOrigin](defSite, instructionToString, color)
                )
            }).toMap

        // 2. create edges
        defSites foreach { lvar ⇒
            val thisNode = nodes(lvar)
            val usages = used(lvar + parametersOffset)
            if (usages == null || usages.isEmpty)
                unusedNode.addChild(thisNode)
            else
                usages.foreach { usage ⇒
                    val usageNode = nodes.get(usage)
                    if (usageNode.isDefined)
                        usageNode.get.addChild(thisNode)
                    else {
                        val useNode = new DefaultMutableNode[ValueOrigin](usage, instructionToString)
                        useNode.addChild(thisNode)
                        nodes += ((usage, useNode))
                    }

                }
        }

        nodes.values.toSet + unusedNode
    }

    private[this] var lastError = 0l
    abstract override def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): List[PC] = {

        def continuation(newWorklist: List[PC]): List[PC] = super.flow(
            currentPC, successorPC,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            newWorklist,
            operandsArray, localsArray,
            tracer)

        try {
            handleFlow(
                currentPC, successorPC,
                isExceptionalControlFlow, abruptSubroutineTerminationCount, wasJoinPerformed,
                worklist,
                operandsArray,
                tracer,
                continuation)
        } catch {
            case ct: ControlThrowable ⇒ throw ct
            case t: Throwable ⇒
                if ((System.currentTimeMillis() - lastError) > 1000l) {
                    lastError = System.currentTimeMillis()
                    org.opalj.io.writeAndOpen(dumpDefUseInfo, "DefUseInfo", ".html")
                }
                throw t
        }
    }

    private[this] def handleFlow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        tracer: Option[AITracer],
        continuation: (List[PC]) ⇒ List[PC]): List[PC] = {

        var forceScheduling = false
        val instruction = instructions(currentPC)

        def updateUsed(usedVars: Set[Int], useSite: PC): Unit = usedVars.foreach { use ⇒
            val usedIndex = use + parametersOffset
            assert(
                usedIndex >= 0,
                s"unexpected use: $use + $parametersOffset = $usedIndex; initial locals: "+
                    defLocals(0).mapKV((i, l) ⇒
                        if (l eq null)
                            i+": N/A"
                        else
                            l.mkString(s"$i: {", ", ", "}")).mkString("", "; ", ""))

            val oldUsedInfo: PCs = used(usedIndex)
            if (oldUsedInfo eq null)
                used(usedIndex) = UShortSet(useSite)
            else {
                val newUsedInfo = useSite +≈: oldUsedInfo
                if (newUsedInfo ne oldUsedInfo)
                    used(usedIndex) = newUsedInfo
            }
        }

        def propagate(
            newDefOps: List[Set[ValueOrigin]],
            newDefLocals: Registers[Set[ValueOrigin]]): Unit = {
            if (wasJoinPerformed) {

                // we now also have to perform a join...
                @annotation.tailrec def joinDefOps(
                    oldDefOps: List[Set[ValueOrigin]],
                    lDefOps: List[Set[ValueOrigin]],
                    rDefOps: List[Set[ValueOrigin]],
                    oldIsSuperset: Boolean = true,
                    joinedDefOps: List[Set[ValueOrigin]] = Nil): List[Set[ValueOrigin]] = {
                    if (lDefOps.isEmpty) {
                        assert(rDefOps.isEmpty)
                        return if (oldIsSuperset) oldDefOps else joinedDefOps.reverse;
                    }
                    assert(
                        rDefOps.nonEmpty,
                        s"unexpected (pc:$currentPC -> pc:$successorPC): $lDefOps vs. $rDefOps; original: $oldDefOps"
                    )

                    val newHead = lDefOps.head
                    val oldHead = rDefOps.head
                    if (newHead.subsetOf(oldHead))
                        joinDefOps(
                            oldDefOps,
                            lDefOps.tail, rDefOps.tail,
                            oldIsSuperset, oldHead :: joinedDefOps)
                    else
                        joinDefOps(
                            oldDefOps,
                            lDefOps.tail, rDefOps.tail,
                            false, (newHead ++ oldHead) :: joinedDefOps)
                }
                val oldDefOps = defOps(successorPC)
                if (newDefOps ne oldDefOps) {
                    val joinedDefOps = joinDefOps(oldDefOps, newDefOps, oldDefOps)
                    if (joinedDefOps ne oldDefOps) {
                        forceScheduling = true
                        defOps(successorPC) = joinedDefOps
                    }
                }

                val oldDefLocals = defLocals(successorPC)
                if (newDefLocals ne oldDefLocals) {
                    val joinedDefLocals =
                        oldDefLocals.merge(
                            newDefLocals,
                            { (o, n) ⇒
                                if (n eq null)
                                    o
                                else if (o eq null)
                                    n
                                else {
                                    if (n.subsetOf(o))
                                        o
                                    else
                                        n ++ o
                                }
                            })
                    if (joinedDefLocals ne oldDefLocals) {
                        forceScheduling = true
                        defLocals(successorPC) = joinedDefLocals
                    }
                }
            } else {
                defOps(successorPC) = newDefOps
                defLocals(successorPC) = newDefLocals
            }
        }

        /*
         * Specifies that the given number of stack values is used and also popped from
         * the stack and that – optionally – a new value is pushed onto the stack and
         * associated with a new variable.
         */
        def stackOp(usedVarCount: Int, newVarCreated: Boolean): Unit = {
            // Usage is independent of the question whether the usage resulted in an
            // exceptional control flow.
            val currentDefOps = defOps(currentPC)

            val usedOps = currentDefOps.take(usedVarCount)
            usedOps.foreach { op ⇒ updateUsed(op, currentPC) }

            val newDefOps =
                if (isExceptionalControlFlow) {
                    // The stack only contains the exception (that was created before
                    // and explicitly used by a throw instruction) or that resulted from
                    // a called method or that was created by the JVM
                    // (Whether we had a join or not is irrelevant.)
                    val successorDefOps = defOps(successorPC)
                    if (successorDefOps eq null)
                        List(Set(successorPC))
                    else
                        successorDefOps
                } else {
                    if (newVarCreated)
                        Set(currentPC) :: currentDefOps.drop(usedVarCount)
                    else
                        currentDefOps.drop(usedVarCount)
                }

            propagate(newDefOps, defLocals(currentPC))
        }

        def load(index: Int): Unit = {
            // there will never be an exceptional control flow ...
            propagate(
                defLocals(currentPC)(index) :: defOps(currentPC),
                defLocals(currentPC))
        }

        def store(index: Int): Unit = {
            // there will never be an exceptional control flow ...
            propagate(
                defOps(currentPC).tail,
                defLocals(currentPC).updated(index, defOps(currentPC).head)
            )
        }

        // THE IMPLEMENTATION...

        (instruction.opcode: @annotation.switch) match {
            case GOTO.opcode | GOTO_W.opcode |
                NOP.opcode |
                WIDE.opcode |
                RETURN.opcode ⇒
                propagate(defOps(currentPC), defLocals(currentPC))

            case JSR.opcode | JSR_W.opcode ⇒
                stackOp(0, true)

            case RET.opcode ⇒
                val RET(lvIndex) = instruction
                val oldDefLocals = defLocals(currentPC)
                updateUsed(oldDefLocals(lvIndex), currentPC)
                propagate(defOps(currentPC), oldDefLocals)

            case IF_ACMPEQ.opcode | IF_ACMPNE.opcode
                | IF_ICMPEQ.opcode | IF_ICMPNE.opcode
                | IF_ICMPGT.opcode | IF_ICMPGE.opcode | IF_ICMPLT.opcode | IF_ICMPLE.opcode ⇒
                stackOp(2, false)

            case IFNULL.opcode | IFNONNULL.opcode
                | IFEQ.opcode | IFNE.opcode
                | IFGT.opcode | IFGE.opcode | IFLT.opcode | IFLE.opcode
                | LOOKUPSWITCH.opcode | TABLESWITCH.opcode ⇒
                stackOp(1, false)

            case ATHROW.opcode ⇒
                // we have an internally thrown exception...
                stackOp(1, false)

            case NEWARRAY.opcode | ANEWARRAY.opcode | ARRAYLENGTH.opcode ⇒
                stackOp(1, true)

            case MULTIANEWARRAY.opcode ⇒
                val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                stackOp(multianewarray.dimensions, true)

            case 50 /*aaload*/ |
                49 /*daload*/ | 48 /*faload*/ |
                51 /*baload*/ |
                52 /*caload*/ | 46 /*iaload*/ | 47 /*laload*/ | 53 /*saload*/ ⇒
                stackOp(2, true)

            case 83 /*aastore*/ |
                84 /*bastore*/ |
                85 /*castore*/ | 79 /*iastore*/ | 80 /*lastore*/ | 86 /*sastore*/ |
                82 /*dastore*/ | 81 /*fastore*/ ⇒
                stackOp(3, false)

            case 180 /*getfield*/     ⇒ stackOp(1, true)
            case 178 /*getstatic*/    ⇒ stackOp(0, true)
            case 181 /*putfield*/     ⇒ stackOp(2, false)
            case 179 /*putstatic*/    ⇒ stackOp(1, false)

            case 194 /*monitorenter*/ ⇒ stackOp(1, false)
            case 195 /*monitorexit*/  ⇒ stackOp(1, false)

            //
            // METHOD INVOCATIONS
            //
            case 184 /*invokestatic*/ | 186 /*invokedynamic*/ |
                185 /*invokeinterface*/ | 183 /*invokespecial*/ | 182 /*invokevirtual*/ ⇒
                val invoke = instruction.asInstanceOf[InvocationInstruction]
                val descriptor = invoke.methodDescriptor
                stackOp(
                    invoke.numberOfPoppedOperands(NullFunction),
                    !descriptor.returnType.isVoidType)

            case 25 /*aload*/ | 24 /*dload*/ | 23 /*fload*/ | 21 /*iload*/ | 22 /*lload*/ ⇒
                val lvIndex = instruction.asInstanceOf[LoadLocalVariableInstruction].lvIndex
                load(lvIndex)
            case 42 /*aload_0*/ | 38 /*dload_0*/ | 34 /*fload_0*/ | 26 /*iload_0*/ | 30 /*lload_0*/ ⇒
                load(0)
            case 43 /*aload_1*/ | 39 /*dload_1*/ | 35 /*fload_1*/ | 27 /*iload_1*/ | 31 /*lload_1*/ ⇒
                load(1)
            case 44 /*aload_2*/ | 40 /*dload_2*/ | 36 /*fload_2*/ | 28 /*iload_2*/ | 32 /*lload_2*/ ⇒
                load(2)
            case 45 /*aload_3*/ | 41 /*dload_3*/ | 37 /*fload_3*/ | 29 /*iload_3*/ | 33 /*lload_3*/ ⇒
                load(3)

            case 58 /*astore*/ | 57 /*dstore*/ | 56 /*fstore*/ | 54 /*istore*/ | 55 /*lstore*/ ⇒
                val lvIndex = instruction.asInstanceOf[StoreLocalVariableInstruction].lvIndex
                store(lvIndex)
            case 75 /*astore_0*/ | 71 /*dstore_0*/ | 67 /*fstore_0*/ | 63 /*lstore_0*/ | 59 /*istore_0*/ ⇒
                store(0)
            case 76 /*astore_1*/ | 72 /*dstore_1*/ | 68 /*fstore_1*/ | 64 /*lstore_1*/ | 60 /*istore_1*/ ⇒
                store(1)
            case 77 /*astore_2*/ | 73 /*dstore_2*/ | 69 /*fstore_2*/ | 65 /*lstore_2*/ | 61 /*istore_2*/ ⇒
                store(2)
            case 78 /*astore_3*/ | 74 /*dstore_3*/ | 70 /*fstore_3*/ | 66 /*lstore_3*/ | 62 /*istore_3*/ ⇒
                store(3)

            //
            // PUSH CONSTANT VALUE
            //

            case 1 /*aconst_null*/ | 16 /*bipush*/ |
                14 /*dconst_0*/ | 15 /*dconst_1*/ |
                11 /*fconst_0*/ | 12 /*fconst_1*/ | 13 /*fconst_2*/ |
                2 /*iconst_m1*/ |
                3 /*iconst_0*/ | 4 /*iconst_1*/ | 5 /*iconst_2*/ | 6 /*iconst_3*/ | 7 /*iconst_4*/ | 8 /*iconst_5*/ |
                9 /*lconst_0*/ | 10 /*lconst_1*/ |
                18 /*ldc*/ | 19 /*ldc_w*/ | 20 /*ldc2_w*/ | 17 /*sipush*/ ⇒
                stackOp(0, true)

            //
            // RELATIONAL OPERATORS
            //
            case 150 /*fcmpg*/ | 149 /*fcmpl*/ | 152 /*dcmpg*/ | 151 /*dcmpl*/ | 148 /*lcmp*/ ⇒
                stackOp(2, true)

            //
            // UNARY EXPRESSIONS
            //
            case 116 /*ineg*/ | 117 /*lneg*/ | 119 /*dneg*/ | 118 /*fneg*/ ⇒
                stackOp(1, true)

            case 99 /*dadd*/ | 111 /*ddiv*/ | 107 /*dmul*/ | 115 /*drem*/ | 103 /*dsub*/ |
                98 /*fadd*/ | 110 /*fdiv*/ | 106 /*fmul*/ | 114 /*frem*/ | 102 /*fsub*/ |
                109 /*ldiv*/ | 105 /*lmul*/ | 113 /*lrem*/ | 101 /*lsub*/ | 97 /*ladd*/ |
                96 /*iadd*/ | 108 /*idiv*/ | 104 /*imul*/ | 112 /*irem*/ | 100 /*isub*/ |
                126 /*iand*/ | 128 /*ior*/ | 130 /*ixor*/ |
                127 /*land*/ | 129 /*lor*/ | 131 /*lxor*/ |
                120 /*ishl*/ | 122 /*ishr*/ | 124 /*iushr*/ |
                121 /*lshl*/ | 123 /*lshr*/ | 125 /*lushr*/ ⇒
                stackOp(2, true)

            //
            // GENERIC STACK MANIPULATION
            //
            case 89 /*dup*/ ⇒
                val oldDefOps = defOps(currentPC)
                propagate(oldDefOps.head :: oldDefOps, defLocals(currentPC))
            case 90 /*dup_x1*/ ⇒
                val v1 :: v2 :: rest = defOps(currentPC)
                propagate(v1 :: v2 :: v1 :: rest, defLocals(currentPC))
            case 91 /*dup_x2*/ ⇒
                operandsArray(currentPC) match {
                    case (v1 /*@ CTC1()*/ ) :: (v2 @ CTC1()) :: _ ⇒
                        val (v1 :: v2 :: v3 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v3 :: v1 :: rest, defLocals(currentPC))
                    case _ ⇒
                        val (v1 :: v2 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v1 :: rest, defLocals(currentPC))
                }
            case 92 /*dup2*/ ⇒
                operandsArray(currentPC) match {
                    case (v1 @ CTC1()) :: _ ⇒
                        val currentDefOps = defOps(currentPC)
                        val (v1 :: v2 :: _) = currentDefOps
                        propagate(v1 :: v2 :: currentDefOps, defLocals(currentPC))
                    case _ ⇒
                        val oldDefOps = defOps(currentPC)
                        propagate(oldDefOps.head :: defOps(currentPC), defLocals(currentPC))
                }
            case 93 /*dup2_x1*/ ⇒
                operandsArray(currentPC) match {
                    case (v1 @ CTC1()) :: _ ⇒
                        val (v1 :: v2 :: v3 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v3 :: v1 :: v2 :: rest, defLocals(currentPC))
                    case _ ⇒
                        val (v1 :: v2 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v1 :: rest, defLocals(currentPC))
                }
            case 94 /*dup2_x2*/ ⇒
                operandsArray(currentPC) match {
                    case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: _ ⇒
                        val (v1 :: v2 :: v3 :: v4 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest, defLocals(currentPC))
                    case (v1 @ CTC1()) :: (v2 @ CTC1()) :: _ ⇒
                        val (v1 :: v2 :: v3 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v3 :: v1 :: v2 :: rest, defLocals(currentPC))
                    case (v1 /* @ CTC2()*/ ) :: (v2 @ CTC1()) :: _ ⇒
                        val (v1 :: v2 :: v3 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v3 :: v1 :: rest, defLocals(currentPC))
                    case _ ⇒
                        val (v1 :: v2 :: rest) = defOps(currentPC)
                        propagate(v1 :: v2 :: v1 :: rest, defLocals(currentPC))
                }

            case 87 /*pop*/ ⇒
                propagate(defOps(currentPC).tail, defLocals(currentPC))
            case 88 /*pop2*/ ⇒
                if (operandsArray(currentPC).head.computationalType.operandSize == 1)
                    propagate(defOps(currentPC).drop(2), defLocals(currentPC))
                else
                    propagate(defOps(currentPC).tail, defLocals(currentPC))

            case 95 /*swap*/ ⇒ {
                val v1 :: v2 :: rest = defOps(currentPC)
                propagate(v2 :: v1 :: rest, defLocals(currentPC))
            }

            case 144 /*d2f*/ | 142 /*d2i*/ | 143 /*d2l*/ |
                141 /*f2d*/ | 139 /*f2i*/ | 140 /*f2l*/ |
                145 /*i2b*/ | 146 /*i2c*/ | 135 /*i2d*/ | 134 /*i2f*/ | 133 /*i2l*/ | 147 /*i2s*/ |
                138 /*l2d*/ | 137 /*l2f*/ | 136 /*l2i*/ |
                193 /*instanceof*/ ⇒
                stackOp(1, true)

            case CHECKCAST.opcode ⇒
                // we "just" inspect the top-most stack value
                val currentDefOps = defOps(currentPC)
                updateUsed(currentDefOps.head, currentPC)
                val newDefOps =
                    if (isExceptionalControlFlow) {
                        val newDefOps = defOps(successorPC)
                        if (newDefOps eq null)
                            List(Set(successorPC))
                        else
                            newDefOps
                    } else
                        defOps(currentPC)
                propagate(newDefOps, defLocals(currentPC))

            case 132 /*iinc*/ ⇒
                val IINC(index, _) = instruction
                val currentDefLocals = defLocals(currentPC)
                updateUsed(currentDefLocals(index), currentPC)
                propagate(
                    defOps(currentPC),
                    currentDefLocals.updated(index, Set.empty + currentPC))

            case 187 /*new*/ ⇒
                stackOp(0, true)

            // Should never occur...
            case 176 /*areturn*/ |
                175 /*dreturn*/ | 174 /*freturn*/ | 172 /*ireturn*/ | 173 /*lreturn*/ ⇒
                val message = s"unexpected return instruction"
                throw BytecodeProcessingFailedException(message)

            case opcode ⇒
                val message = s"unknown opcode: $opcode"
                throw BytecodeProcessingFailedException(message)
        }

        (instructions(successorPC).opcode: @annotation.switch) match {
            case ARETURN.opcode |
                DRETURN.opcode | FRETURN.opcode | IRETURN.opcode | LRETURN.opcode |
                ATHROW.opcode ⇒
                updateUsed(defOps(successorPC).head, successorPC)

            case _ ⇒ /* let's continue with the standard handling */
        }

        def schedule(): List[PC] = {
            if (abruptSubroutineTerminationCount > 0) {
                var header: List[PC] = Nil
                val relevantWorklist = {
                    var subroutinesToTerminate = abruptSubroutineTerminationCount
                    worklist.dropWhile { pc ⇒
                        if (pc == SUBROUTINE) {
                            subroutinesToTerminate -= 1
                            if (subroutinesToTerminate > 0) {
                                header = pc :: header
                                true
                            } else {
                                false
                            }
                        } else {
                            header = pc :: header
                            true
                        }
                    }.tail /* drop SUBROUTINE MARKER */
                }
                if (containsInPrefix(relevantWorklist, successorPC, SUBROUTINE_START)) {
                    worklist
                } else {
                    header.reverse ::: (SUBROUTINE :: successorPC :: relevantWorklist)
                }
            } else {
                if (containsInPrefix(worklist, successorPC, SUBROUTINE_START)) {
                    worklist
                } else {
                    successorPC :: worklist
                }
            }
        }

        if (forceScheduling) {
            val newWorklist = schedule()
            if ((worklist ne newWorklist) && tracer.isDefined) {
                // the instruction was not yet scheduled for another evaluation
                tracer.get.flow(domain)(currentPC, successorPC, isExceptionalControlFlow)
            }
            continuation(newWorklist)
        } else {
            continuation(worklist)
        }
    }
}
private object NullFunction extends (Int ⇒ ComputationalTypeCategory) {
    def apply(i: Int): Nothing = throw new UnsupportedOperationException
}
