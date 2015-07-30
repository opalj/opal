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
import org.opalj.collection.mutable.SmallValuesSet
import scala.collection.mutable.Queue

/**
 * Collects the abstract interpretation time Definition-Use information.
 * I.e., makes the information available
 * which value is accessed where. Here, all values (variables) are identified using int
 * values where the int value is equivalent to the pc of the instruction that
 * initializes the respective (virtual) variable.
 *
 * The parameters given to a method have negative `int` values (the first
 * parameter has the value -1, the second -2 and so forth; the computational type
 * category is ignored.).
 *
 * ==Usage==
 * This trait collects the def/use information after the abstract interpretation has
 * ended.
 *
 * ==Core Properties==
 * === Reusability ===
 * This domain can be reused to successively perform abstract interpretations of different
 * methods. The domain's inherited `initProperties` method – which is called by the AI
 * framework –  resets the entire state related
 * to the abstract interpretation of a method.
 *
 * @author Michael Eichberg
 */
trait RecordDefUse extends RecordCFG { defUseDomain: Domain with TheCode ⇒

    // IDEA:
    // EACH LOCAL VARIABLE IS NAMED USING THE PC OF THE INSTRUCTION THAT INITIALIZES IT.
    //
    // EXAMPLE (AFTER COMPUTING THE DEF/USE INFORMATION)
    // PC:                0:        1:          2:           3:        4:         5:
    // INSTRUCTION        a_load0   getfield    invokestatic a_store0  getstatic  return
    // STACK              empty     [ -1 ]      [ 1 ]        [ 2 ]     empty      [ 4 ]
    // REGISTERS          0: -1     0: -1       0: -1        0: -1     0: 2       0: 1
    // USED(BY) "-1":{1}  "0": N/A  "1":{2}     "2":{3}      "3": N/A  "4": {5}   "5": N/A

    type PCs = UShortSet
    type ValueOrigins = SmallValuesSet

    /**
     * Creates a new set for storing value origins that contains the given origin value.
     */
    @inline private[this] final def ValueOrigins(
        min: Int = this.min,
        max: Int = this.max,
        origin: Int): SmallValuesSet =
        SmallValuesSet.create(min, max, origin)

    private[this] var min: Int = _ // initialized by initProperties
    private[this] var max: Int = _ // initialized by initProperties

    private[this] var instructions: Array[Instruction] = _ // initialized by initProperties

    // Stores the information where the value defined by an instruction is
    // used. The used array basically mirrors the instructions array, but has additional
    // space for storing the information about the usage of the parameters. The size
    // of this additional space is `parametersOffset` large and is prepended to
    // the array that mirrors the instructions array.
    private[this] var used: Array[ValueOrigins] = _ // initialized by initProperties
    private[this] var parametersOffset: Int = _ // initialized by initProperties

    // This array contains the information where each operand value found at a
    // specific instruction was defined.
    private[this] var defOps: Array[List[ValueOrigins]] = _
    // This array contains the information where each local is defined;
    // negative values indicate that the values are parameters.
    private[this] var defLocals: Array[Registers[ValueOrigins]] = _

    abstract override def initProperties(
        code: Code,
        joinInstructions: BitSet,
        locals: Locals): Unit = {

        instructions = code.instructions
        val codeSize = instructions.size
        // The following value for min  is a conservative approx. which may lead to the
        // use of a SmallValuesArray that can actually store larger values than
        // necessary; however, this will occur only in a very small number of cases.
        val absoluteMin = -code.maxLocals
        val defOps = new Array[List[ValueOrigins]](codeSize)
        defOps(0) = Nil // the operand stack is empty...
        this.defOps = defOps

        // initialize initial def-use information based on the parameters
        val defLocals = new Array[Registers[ValueOrigins]](codeSize)
        var parameterIndex = 0
        defLocals(0) =
            locals.map { v ⇒
                parameterIndex -= 1 // to get the same offsets as used by the AI for parameters
                if (v ne null) {
                    ValueOrigins(absoluteMin, /*max*/ codeSize, parameterIndex)
                } else {
                    null
                }
            }
        this.defLocals = defLocals
        this.min = parameterIndex
        this.max = codeSize
        this.parametersOffset = -parameterIndex

        this.used = new Array(codeSize + parametersOffset)

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

    /**
     * Creates an XHTML document that contains information about the def-/use
     * information.
     */
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
                        <td>{ i }<br/>{ instructions(i).toString(i) }</td>
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

    /**
     * Returns the instructions which use the value with the given value origin.
     */
    def usedBy(valueOrigin: ValueOrigin): ValueOrigins = used(valueOrigin + parametersOffset)

    /**
     * Returns the instruction which defined the value used by the instruction with the given `pc` and which
     * is stored at the stack position with the given valueIndex. The first value on
     * the stack has index 0.
     */
    def operandOrigin(pc: PC, valueIndex: Int): ValueOrigins = defOps(pc)(valueIndex)

    /**
     * Returns the instructions which define the value found in the register variable with
     * index `valueIndex` and the program counter `pc`.
     */
    def localOrigin(pc: PC, valueIndex: Int): ValueOrigins = defLocals(pc)(valueIndex)

    /**
     * Creates a multi-graph that represents the method's def-use information. I.e.,
     * in which way a certain value is used by other instructions and who the derived
     * values are in turn used by further instructions.
     */
    def createDefUseGraph(code: Code): Set[DefaultMutableNode[ValueOrigin]] = {

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

        def instructionToString(vo: ValueOrigin): String = {
            if (vo < 0)
                s"<parameter:${-vo - 1}>"
            else
                s"$vo: "+code.instructions(vo).toString(vo)
        }

        val unusedNode =
            new DefaultMutableNode(
                Int.MinValue: ValueOrigin, (vo: ValueOrigin) ⇒ "<NONE>", Some("orange")
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
            if ((usages eq null) || usages.isEmpty)
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

    /**
     * The method which computes the def/use information when the instruction with
     * the pc `successorPC` is executed immediately after the instruction with `currentPC`.
     */
    private[this] def handleFlow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        joinInstructions: BitSet,
        operandsArray: OperandsArray): Boolean = {

        var forceScheduling = false
        val instruction = instructions(currentPC)
        val successorInstruction = instructions(successorPC)

        //
        // HELPER METHODS
        //
        def updateUsageInformation(usedValues: ValueOrigins, useSite: PC): Unit = {
            usedValues foreach { usedValue ⇒
                val usedIndex = usedValue + parametersOffset

                val oldUsedInfo: ValueOrigins = used(usedIndex)
                if (oldUsedInfo eq null) {
                    used(usedIndex) = SmallValuesSet.create(max, useSite)
                } else {
                    val newUsedInfo = useSite +≈: oldUsedInfo
                    if (newUsedInfo ne oldUsedInfo)
                        used(usedIndex) = newUsedInfo
                }
            }
        }

        def propagate(
            newDefOps: List[ValueOrigins],
            newDefLocals: Registers[ValueOrigins]): Boolean = {
            if (joinInstructions.contains(successorPC) && (defLocals(successorPC) ne null)) {

                // we now also have to perform a join...
                @annotation.tailrec def joinDefOps(
                    oldDefOps: List[ValueOrigins],
                    lDefOps: List[ValueOrigins],
                    rDefOps: List[ValueOrigins],
                    oldIsSuperset: Boolean = true,
                    joinedDefOps: List[ValueOrigins] = Nil): List[ValueOrigins] = {
                    if (lDefOps.isEmpty) {
                        //                        assert(rDefOps.isEmpty)
                        return if (oldIsSuperset) oldDefOps else joinedDefOps.reverse;
                    }
                    // assert(
                    //     rDefOps.nonEmpty,
                    //     s"unexpected (pc:$currentPC -> pc:$successorPC): $lDefOps vs. $rDefOps; original: $oldDefOps"
                    // )

                    val newHead = lDefOps.head
                    val oldHead = rDefOps.head
                    if (newHead.subsetOf(oldHead))
                        joinDefOps(
                            oldDefOps,
                            lDefOps.tail, rDefOps.tail,
                            oldIsSuperset, oldHead :: joinedDefOps)
                    else {
                        val joinedHead = (newHead ++ oldHead)
                        //                        assert(newHead.subsetOf(joinedHead))
                        //                        assert(oldHead.subsetOf(joinedHead), s"$newHead ++ $oldHead is $joinedHead")
                        //                        assert(joinedHead.size > oldHead.size, s"$newHead ++  $oldHead is $joinedHead")
                        joinDefOps(
                            oldDefOps,
                            lDefOps.tail, rDefOps.tail,
                            false, joinedHead :: joinedDefOps)
                    }
                }

                val oldDefOps = defOps(successorPC)
                if (newDefOps ne oldDefOps) {
                    val joinedDefOps = joinDefOps(oldDefOps, newDefOps, oldDefOps)
                    if (joinedDefOps ne oldDefOps) {
                        // assert(
                        //     joinedDefOps != oldDefOps,
                        //     s"$joinedDefOps is (unexpectedly) equal to $newDefOps join $oldDefOps")
                        forceScheduling =
                            // There is nothing to propagate beyond the next
                            // instruction if the next one is a "return" instruction.
                            !successorInstruction.isInstanceOf[ReturnInstruction]
                        defOps(successorPC) = joinedDefOps
                    }
                }

                val oldDefLocals = defLocals(successorPC)
                if (newDefLocals ne oldDefLocals) {
                    // newUsage is `true` if a new value(variable) may be used somewhere
                    // (I)
                    // For example:
                    // 0: ALOAD_0
                    // 1: INVOKEVIRTUAL com.sun.media.sound.EventDispatcher dispatchEvents (): void
                    // 4: GOTO 0↑
                    // 7: ASTORE_1
                    // 8: GOTO 0↑
                    // The last goto leads to some new information regarding the values
                    // on the stack (e.g., Register 1 now contains an exception), but
                    // propagating this information is useless - the value is never
                    // used...
                    // (II)
                    // Furthermore, whenever we have a jump back to the first instruction
                    // (PC == 0) and the joined values are unrelated to the parameters
                    // - i.e., we do not assign a new value to a register used by a
                    // parameter -
                    // then we do not have to force a scheduling of the reevaluation of
                    // the next instruction
                    // since there has to be some assignment related to the respective
                    // variables (there is no load without a previous store).
                    var newUsage = false
                    val joinedDefLocals =
                        oldDefLocals.merge(newDefLocals,
                            { (o, n) ⇒
                                // In general, if n or o equals null, then
                                // the register variable did not contain any
                                // useful information when the current instruction was
                                // reached for the first time, hence there will
                                // always be an initialization before the next
                                // use of the register value and we can drop all
                                // information.... unless we have a JSR/RET.
                                if (o eq null) {
                                    if ((n ne null) &&
                                        joinInstructions.contains(successorPC) &&
                                        instructions(currentPC).isInstanceOf[JSRInstruction]) {
                                        newUsage = true
                                        n
                                    } else {
                                        null
                                    }
                                } else if (n eq null) {
                                    if ((o ne null) &&
                                        joinInstructions.contains(successorPC) &&
                                        instructions(currentPC).isInstanceOf[JSRInstruction]) {
                                        newUsage = true
                                        o
                                    } else {
                                        null
                                    }
                                } else if (n subsetOf o) {
                                    o
                                } else {
                                    newUsage = true
                                    val joinedDefLocals = n ++ o
                                    //                                    assert(joinedDefLocals.size > o.size, s"$n ++  $o is $joinedDefLocals")
                                    joinedDefLocals
                                }
                            })
                    if (joinedDefLocals ne oldDefLocals) {
                        // assert(
                        //      joinedDefLocals != oldDefLocals,
                        //      s"$joinedDefLocals is (unexpectedly) equal to $newDefLocals join $oldDefLocals")
                        forceScheduling = forceScheduling || {
                            // There is nothing to do if all joins are related to unused vars...
                            newUsage &&
                                // There is nothing to propagate if the next
                                // instruction is a "return" instruction.
                                !successorInstruction.isInstanceOf[ReturnInstruction]

                        }
                        defLocals(successorPC) = joinedDefLocals
                    }
                }

                forceScheduling
            } else {
                defOps(successorPC) = newDefOps
                defLocals(successorPC) = newDefLocals
                true // <=> always schedule the execution of the next instruction
            }
        }

        /*
         * Specifies that the given number of stack values is used and also popped from
         * the stack and that – optionally – a new value is pushed onto the stack (and
         * associated with a new variable).
         */
        def stackOp(usedValues: Int, pushesValue: Boolean): Boolean = {
            // Usage is independent of the question whether the usage resulted in an
            // exceptional control flow.
            val currentDefOps = defOps(currentPC)
            forFirstN(currentDefOps, usedValues) { op ⇒
                updateUsageInformation(op, currentPC)
            }

            val newDefOps =
                if (isExceptionalControlFlow) {
                    // The stack only contains the exception (that was created before
                    // and explicitly used by a throw instruction) or that resulted from
                    // a called method or that was created by the JVM
                    // (Whether we had a join or not is irrelevant.)
                    val successorDefOps = defOps(successorPC)
                    if (successorDefOps eq null)
                        List(ValueOrigins(origin = successorPC))
                    else {
                        //                        assert(successorDefOps.tail.isEmpty)
                        successorDefOps
                    }
                } else {
                    if (pushesValue)
                        ValueOrigins(origin = currentPC) :: currentDefOps.drop(usedValues)
                    else
                        currentDefOps.drop(usedValues)
                }

            propagate(newDefOps, defLocals(currentPC))
        }

        def load(index: Int): Boolean = {
            // there will never be an exceptional control flow ...
            val currentLocals = defLocals(currentPC)
            propagate(currentLocals(index) :: defOps(currentPC), currentLocals)
        }

        def store(index: Int): Boolean = {
            // there will never be an exceptional control flow ...
            val currentOps = defOps(currentPC)
            val newDefLocals = defLocals(currentPC).updated(index, currentOps.head)
            propagate(currentOps.tail, newDefLocals)
        }

        //
        // THE IMPLEMENTATION...
        //
        val scheduleNextPC: Boolean = (instruction.opcode: @annotation.switch) match {
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
                val returnAddressValue = oldDefLocals(lvIndex)
                updateUsageInformation(returnAddressValue, currentPC)
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

            //
            // ARRAYS
            //
            case NEWARRAY.opcode | ANEWARRAY.opcode ⇒
                stackOp(1 /*count*/ , pushesValue = true)

            case ARRAYLENGTH.opcode ⇒ stackOp(1, pushesValue = true)

            case MULTIANEWARRAY.opcode ⇒
                val dimensions = instruction.asInstanceOf[MULTIANEWARRAY].dimensions
                stackOp(dimensions, pushesValue = true)

            case 50 /*aaload*/ |
                49 /*daload*/ | 48 /*faload*/ |
                51 /*baload*/ |
                52 /*caload*/ | 46 /*iaload*/ | 47 /*laload*/ | 53 /*saload*/ ⇒
                stackOp(2, pushesValue = true)

            case 83 /*aastore*/ |
                84 /*bastore*/ |
                85 /*castore*/ | 79 /*iastore*/ | 80 /*lastore*/ | 86 /*sastore*/ |
                82 /*dastore*/ | 81 /*fastore*/ ⇒
                stackOp(3, pushesValue = false)

            //
            // FIELD ACCESS
            //
            case 180 /*getfield*/     ⇒ stackOp(1, pushesValue = true)
            case 178 /*getstatic*/    ⇒ stackOp(0, pushesValue = true)
            case 181 /*putfield*/     ⇒ stackOp(2, pushesValue = false)
            case 179 /*putstatic*/    ⇒ stackOp(1, pushesValue = false)

            //
            // MONITOR
            //

            case 194 /*monitorenter*/ ⇒ stackOp(1, pushesValue = false)
            case 195 /*monitorexit*/  ⇒ stackOp(1, pushesValue = false)

            //
            // METHOD INVOCATIONS
            //
            case 184 /*invokestatic*/ | 186 /*invokedynamic*/ |
                185 /*invokeinterface*/ | 183 /*invokespecial*/ | 182 /*invokevirtual*/ ⇒
                val invoke = instruction.asInstanceOf[InvocationInstruction]
                val descriptor = invoke.methodDescriptor
                stackOp(
                    invoke.numberOfPoppedOperands(UnsupportedOperationComputationalTypeCategory),
                    !descriptor.returnType.isVoidType)

            //
            // LOAD AND STORE INSTRUCTIONS
            //
            case 25 /*aload*/ | 24 /*dload*/ | 23 /*fload*/ | 21 /*iload*/ | 22 /*lload*/ ⇒
                load(instruction.asInstanceOf[LoadLocalVariableInstruction].lvIndex)
            case 42 /*aload_0*/ |
                38 /*dload_0*/ | 34 /*fload_0*/ | 26 /*iload_0*/ | 30 /*lload_0*/ ⇒
                load(0)
            case 43 /*aload_1*/ |
                39 /*dload_1*/ | 35 /*fload_1*/ | 27 /*iload_1*/ | 31 /*lload_1*/ ⇒
                load(1)
            case 44 /*aload_2*/ |
                40 /*dload_2*/ | 36 /*fload_2*/ | 28 /*iload_2*/ | 32 /*lload_2*/ ⇒
                load(2)
            case 45 /*aload_3*/ |
                41 /*dload_3*/ | 37 /*fload_3*/ | 29 /*iload_3*/ | 33 /*lload_3*/ ⇒
                load(3)

            case 58 /*astore*/ |
                57 /*dstore*/ | 56 /*fstore*/ | 54 /*istore*/ | 55 /*lstore*/ ⇒
                store(instruction.asInstanceOf[StoreLocalVariableInstruction].lvIndex)
            case 75 /*astore_0*/ |
                71 /*dstore_0*/ | 67 /*fstore_0*/ | 63 /*lstore_0*/ | 59 /*istore_0*/ ⇒
                store(0)
            case 76 /*astore_1*/ |
                72 /*dstore_1*/ | 68 /*fstore_1*/ | 64 /*lstore_1*/ | 60 /*istore_1*/ ⇒
                store(1)
            case 77 /*astore_2*/ |
                73 /*dstore_2*/ | 69 /*fstore_2*/ | 65 /*lstore_2*/ | 61 /*istore_2*/ ⇒
                store(2)
            case 78 /*astore_3*/ |
                74 /*dstore_3*/ | 70 /*fstore_3*/ | 66 /*lstore_3*/ | 62 /*istore_3*/ ⇒
                store(3)

            //
            // PUSH CONSTANT VALUE
            //
            case 1 /*aconst_null*/ |
                2 /*iconst_m1*/ |
                3 /*iconst_0*/ | 4 /*iconst_1*/ |
                5 /*iconst_2*/ | 6 /*iconst_3*/ | 7 /*iconst_4*/ | 8 /*iconst_5*/ |
                9 /*lconst_0*/ | 10 /*lconst_1*/ |
                11 /*fconst_0*/ | 12 /*fconst_1*/ | 13 /*fconst_2*/ |
                14 /*dconst_0*/ | 15 /*dconst_1*/ |
                16 /*bipush*/ | 17 /*sipush*/ |
                18 /*ldc*/ | 19 /*ldc_w*/ | 20 /*ldc2_w*/ ⇒
                stackOp(0, pushesValue = true)

            //
            // RELATIONAL OPERATORS
            //
            case 148 /*lcmp*/ |
                150 /*fcmpg*/ | 149 /*fcmpl*/ |
                152 /*dcmpg*/ | 151 /*dcmpl*/ ⇒
                stackOp(2, pushesValue = true)

            //
            // UNARY EXPRESSIONS
            //
            case 116 /*ineg*/ | 117 /*lneg*/ | 119 /*dneg*/ | 118 /*fneg*/ ⇒
                stackOp(1, pushesValue = true)

            case NEW.opcode ⇒ stackOp(0, pushesValue = true)

            //
            // BINARY EXPRESSIONS
            //
            case IINC.opcode ⇒
                val IINC(index, _) = instruction
                val currentDefLocals = defLocals(currentPC)
                updateUsageInformation(currentDefLocals(index), currentPC)
                val newOrigin = ValueOrigins(origin = currentPC)
                val newDefLocals = currentDefLocals.updated(index, newOrigin)
                propagate(defOps(currentPC), newDefLocals)

            case 99 /*dadd*/ | 111 /*ddiv*/ | 107 /*dmul*/ | 115 /*drem*/ | 103 /*dsub*/ |
                98 /*fadd*/ | 110 /*fdiv*/ | 106 /*fmul*/ | 114 /*frem*/ | 102 /*fsub*/ |
                109 /*ldiv*/ | 105 /*lmul*/ | 113 /*lrem*/ | 101 /*lsub*/ | 97 /*ladd*/ |
                96 /*iadd*/ | 108 /*idiv*/ | 104 /*imul*/ | 112 /*irem*/ | 100 /*isub*/ |
                126 /*iand*/ | 128 /*ior*/ | 130 /*ixor*/ |
                127 /*land*/ | 129 /*lor*/ | 131 /*lxor*/ |
                120 /*ishl*/ | 122 /*ishr*/ | 124 /*iushr*/ |
                121 /*lshl*/ | 123 /*lshr*/ | 125 /*lushr*/ ⇒
                stackOp(2, pushesValue = true)

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

            //
            // VALUE CONVERSIONS
            //
            case 144 /*d2f*/ | 142 /*d2i*/ | 143 /*d2l*/ |
                141 /*f2d*/ | 139 /*f2i*/ | 140 /*f2l*/ |
                145 /*i2b*/ | 146 /*i2c*/ | 135 /*i2d*/ | 134 /*i2f*/ | 133 /*i2l*/ | 147 /*i2s*/ |
                138 /*l2d*/ | 137 /*l2f*/ | 136 /*l2i*/ |
                193 /*instanceof*/ ⇒
                stackOp(1, true)

            case CHECKCAST.opcode ⇒
                // we "just" inspect the top-most stack value
                val currentDefOps = defOps(currentPC)
                updateUsageInformation(currentDefOps.head, currentPC)
                val newDefOps =
                    if (isExceptionalControlFlow) {
                        val newDefOps = defOps(successorPC)
                        if (newDefOps eq null)
                            List(ValueOrigins(origin = successorPC))
                        else
                            newDefOps
                    } else
                        currentDefOps
                propagate(newDefOps, defLocals(currentPC))

            //
            // "ERROR" HANDLING
            //
            case 176 /*areturn*/ |
                175 /*dreturn*/ | 174 /*freturn*/ | 172 /*ireturn*/ | 173 /*lreturn*/ ⇒
                val message =
                    s"a return instruction ($instruction) cannot be the source of a flow"
                throw BytecodeProcessingFailedException(message)

            case opcode ⇒
                throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
        }

        (successorInstruction.opcode: @annotation.switch) match {
            case ARETURN.opcode |
                DRETURN.opcode | FRETURN.opcode | IRETURN.opcode | LRETURN.opcode |
                ATHROW.opcode ⇒
                updateUsageInformation(defOps(successorPC).head, successorPC)

            case _ ⇒ /* let's continue with the standard handling */
        }

        scheduleNextPC
    }

    abstract override def abstractInterpretationEnded(
        aiResult: AIResult { val domain: defUseDomain.type }): Unit = {
        if (aiResult.wasAborted)
            return ;

        val operandsArray = aiResult.operandsArray
        val joinInstructions = aiResult.joinInstructions

        var iterationCount = 0
        val maxIterationCount = aiResult.code.instructions.size * 50
        var subroutinePCs = Set.empty[PC]
        val nextPCs: Queue[PC] = Queue(0)

        while (nextPCs.nonEmpty || { nextPCs ++ subroutinePCs; subroutinePCs = Set.empty; nextPCs.nonEmpty }) {
            val currPC = nextPCs.dequeue
            iterationCount += 1
            if (iterationCount > maxIterationCount) {
                var s = "\nThe analysis failed! "
                s += ("curr: "+currPC+" ... nextPCs: "+nextPCs+" ... subroutinePCs"+subroutinePCs)
                println(s+"\n"+defOps(currPC)+" ... "+defLocals(currPC))
                if (iterationCount > 1.1 * maxIterationCount) {
                    org.opalj.io.writeAndOpen(dumpDefUseInfo().toString, "defuse", ".html")
                    throw new UnknownError(s)
                }
            }

            def handleSuccessor(isExceptionalControlFlow: Boolean)(succPC: PC): Unit = {
                //println(s"$currPC: ${instructions(currPC).toString(currPC)} : $nextPCs ::: $subroutinePCs")
                val scheduleNextPC = try {
                    handleFlow(
                        currPC, succPC, isExceptionalControlFlow,
                        joinInstructions,
                        operandsArray)

                } catch {
                    case e: Throwable ⇒
                        println("curr: "+currPC+"; succ: "+succPC)
                        println(defOps(currPC)+" ... "+defLocals(currPC))
                        println(e.printStackTrace())
                        org.opalj.io.writeAndOpen(dumpDefUseInfo().toString, "defuse", ".html")
                        throw e
                }

                //                assert(defLocals(succPC) ne null)
                //                assert(defOps(succPC) ne null)

                if (scheduleNextPC && !nextPCs.contains(succPC)) {
                    if (instructions(currPC).isInstanceOf[JSRInstruction]) {
                        subroutinePCs += succPC
                    } else {
                        nextPCs.enqueue(succPC)
                    }
                }
            }

            regularSuccessorsOf(currPC).foreach { handleSuccessor(false) }
            exceptionHandlerSuccessorsOf(currPC).foreach { handleSuccessor(true) }
        }

        super.abstractInterpretationEnded(aiResult)
    }

}

private object UnsupportedOperationComputationalTypeCategory extends (Int ⇒ ComputationalTypeCategory) {

    def apply(i: Int): Nothing = throw new UnsupportedOperationException

}
