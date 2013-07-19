/*
 * License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
 * Models the current execution context of a method. I.e., the operand stack as well as
 * the current values of the registers. The memory layout is automatically maintained by
 * BAT while analyzing a method. If specific knowledge about a value is required, the
 * domain is queried to get the necessary information. This callback mechanism enables
 * the domain to use an arbitrary mechanism to represent values and to steer the
 * analysis.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
// TODO [AI] We need some mechanism to bind values to the domain. But, given the dependent method types are not supported for 
final class MemoryLayout(
        val operands: List[Value], // TODO use Stack
        val locals: IndexedSeq[Value])(
                implicit val domain: Domain) {

    override def toString: String = {
        "MemoryLayout[(domain="+domain.getClass.getName+")\n"+
            operands.mkString("\toperands:", ", ", "\n") +
            locals.zipWithIndex.map(l ⇒ l._2+" -> "+l._1).mkString("\tlocals:\n\t\t", "\n\t\t", "\n")+
            "]"
    }

    private type CTC1 = ComputationalTypeCategory1Value
    private type CTC2 = ComputationalTypeCategory2Value

    /**
     * Updates this memory layout with the given memory layout. Returns this memory
     * layout if this memory layout already subsumes the given memory layout.
     */
    def update(other: MemoryLayout): MemoryLayout = {
        assume(this.operands.size == other.operands.size, "cannot update this memory layout with the given memory layout because the stack size is different (this is in violation of the JVM spec.)")
        assume(this.locals.size == other.locals.size, "this memory layout and the given memory layout cannot be merged due to different number of local variables (registers)")

        var thisRemainingOperands = this.operands
        var otherRemainingOperands = other.operands
        var newOperands = List[Value]() // during the update we build the operands stack in reverse order
        var operandsUpdated = false
        while (thisRemainingOperands.nonEmpty /* the number of operands of both memory layouts is equal */ ) {
            val thisOperand = thisRemainingOperands.head
            val otherOperand = otherRemainingOperands.head
            otherRemainingOperands = otherRemainingOperands.tail
            thisRemainingOperands = thisRemainingOperands.tail

            val newOperand = thisOperand.merge(otherOperand)
            assume(!newOperand.isInstanceOf[NoLegalValue], "merging of stack values led to an illegal value")
            newOperands = newOperand :: newOperands
            if (newOperand ne thisOperand)
                operandsUpdated = true
        }

        var localsUpdated = false

        val maxLocals = locals.size
        val newLocals = new Array[Value](maxLocals)
        var i = 0;
        while (i < maxLocals) {
            val thisLocal = this.locals(i)
            val otherLocal = other.locals(i)
            // The value calculated by "merge" may be the value "NoLegalValue" which means
            // the values in the corresponding register were different (path dependent)
            // on the different paths.
            // If we would have a liveness analysis, we could avoid the use of 
            // "NoLegalValue"
            val newLocal = thisLocal merge otherLocal
            newLocals(i) = newLocal
            if (newLocal ne thisLocal)
                localsUpdated = true
            i += 1
        }

        // return the "new" memory layout
        if (operandsUpdated || localsUpdated) {
            new MemoryLayout(newOperands.reverse, newLocals)
        } else {
            this
        }
    }

    def update(currentPC: Int, instruction: Instruction): MemoryLayout = {
        (instruction.opcode: @annotation.switch) match {

            //
            // PUT LOCAL VARIABLE VALUE ONTO STACK
            //
            case 25 /*aload*/ ⇒
                new MemoryLayout(locals(instruction.asInstanceOf[ALOAD].lvIndex) :: operands, locals)
            case 24 /*dload*/ ⇒
                new MemoryLayout(locals(instruction.asInstanceOf[DLOAD].lvIndex) :: operands, locals)
            case 23 /*fload*/ ⇒
                new MemoryLayout(locals(instruction.asInstanceOf[FLOAD].lvIndex) :: operands, locals)
            case 21 /*iload*/ ⇒
                new MemoryLayout(locals(instruction.asInstanceOf[ILOAD].lvIndex) :: operands, locals)
            case 22 /*lload*/ ⇒
                new MemoryLayout(locals(instruction.asInstanceOf[LLOAD].lvIndex) :: operands, locals)
            case 42 /*aload_0*/ | 38 /*dload_0*/ | 34 /*fload_0*/ | 26 /*iload_0*/ | 30 /*lload_0*/ ⇒
                new MemoryLayout(locals(0) :: operands, locals)
            case 43 /*aload_1*/ | 39 /*dload_1*/ | 35 /*fload_1*/ | 27 /*iload_1*/ | 31 /*lload_1*/ ⇒
                new MemoryLayout(locals(1) :: operands, locals)
            case 44 /*aload_2*/ | 40 /*dload_2*/ | 36 /*fload_2*/ | 28 /*iload_2*/ | 32 /*lload_2*/ ⇒
                new MemoryLayout(locals(2) :: operands, locals)
            case 45 /*aload_3*/ | 41 /*dload_3*/ | 37 /*fload_3*/ | 29 /*iload_3*/ | 33 /*lload_3*/ ⇒
                new MemoryLayout(locals(3) :: operands, locals)

            //
            // STORE OPERAND IN LOCAL VARIABLE
            //
            case 58 /*astore*/ ⇒
                new MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[ASTORE].lvIndex, operands.head))
            case 57 /*dstore*/ ⇒
                new MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[DSTORE].lvIndex, operands.head))
            case 56 /*fstore*/ ⇒
                new MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[FSTORE].lvIndex, operands.head))
            case 54 /*istore*/ ⇒
                new MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[ISTORE].lvIndex, operands.head))
            case 55 /*lstore*/ ⇒
                new MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[LSTORE].lvIndex, operands.head))
            case 75 /*astore_0*/
                | 71 /*dstore_0*/
                | 67 /*fstore_0*/
                | 63 /*lstore_0*/
                | 59 /*istore_0*/ ⇒
                new MemoryLayout(operands.tail, locals.updated(0, operands.head))
            case 76 /*astore_1*/
                | 72 /*dstore_1*/
                | 68 /*fstore_1*/
                | 64 /*lstore_1*/
                | 60 /*istore_1*/ ⇒
                new MemoryLayout(operands.tail, locals.updated(1, operands.head))
            case 77 /*astore_2*/
                | 73 /*dstore_2*/
                | 69 /*fstore_2*/
                | 65 /*lstore_2*/
                | 61 /*istore_2*/ ⇒
                new MemoryLayout(operands.tail, locals.updated(2, operands.head))
            case 78 /*astore_3*/
                | 74 /*dstore_3*/
                | 70 /*fstore_3*/
                | 66 /*lstore_3*/
                | 62 /*istore_3*/ ⇒
                new MemoryLayout(operands.tail, locals.updated(3, operands.head))

            //
            // CREATE ARRAY
            //

            case 188 /*newarray*/ ⇒ {
                val count :: rest = operands
                val newOperands = ((instruction.asInstanceOf[NEWARRAY].atype: @annotation.switch) match {
                    case 4 /*BooleanType.atype*/  ⇒ domain.newarray(count, BooleanType)
                    case 5 /*CharType.atype*/     ⇒ domain.newarray(count, CharType)
                    case 6 /*FloatType.atype*/    ⇒ domain.newarray(count, FloatType)
                    case 7 /*DoubleType.atype*/   ⇒ domain.newarray(count, DoubleType)
                    case 8 /*ByteType.atype*/     ⇒ domain.newarray(count, ByteType)
                    case 9 /*ShortType.atype*/    ⇒ domain.newarray(count, ShortType)
                    case 10 /*IntegerType.atype*/ ⇒ domain.newarray(count, IntegerType)
                    case 11 /*LongType.atype*/    ⇒ domain.newarray(count, LongType)
                    case _                        ⇒ sys.error("internal implementation error or invalid bytecode")
                }) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 189 /*anewarray*/ ⇒ {
                val count :: rest = operands
                val newOperands = domain.newarray(count, instruction.asInstanceOf[ANEWARRAY].componentType) :: rest
                new MemoryLayout(newOperands, locals)
            }

            case 197 /*multianewarray*/ ⇒ {
                val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                val initDimensions = operands.take(multianewarray.dimensions)
                new MemoryLayout(
                    domain.multianewarray(initDimensions, multianewarray.componentType) :: (operands.drop(multianewarray.dimensions)),
                    locals)
            }

            //
            // LOAD FROM AND STORE VALUE IN ARRAYS
            //

            case 50 /*aaload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.aaload(index, arrayref) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 83 /*aastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.aastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 51 /*baload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.baload(index, arrayref) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 84 /*bastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.bastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 52 /*caload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.caload(index, arrayref) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 85 /*castore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.castore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 49 /*daload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.daload(index, arrayref) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 82 /*dastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.dastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 48 /*faload*/ ⇒ {
                val index :: arrayref :: rest = operands
                new MemoryLayout(domain.faload(index, arrayref) :: rest, locals)
            }
            case 81 /*fastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.fastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 46 /*iaload*/ ⇒ {
                val index :: arrayref :: rest = operands
                new MemoryLayout(domain.iaload(index, arrayref) :: rest, locals)
            }
            case 79 /*iastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.iastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 47 /*laload*/ ⇒ {
                val index :: arrayref :: rest = operands
                new MemoryLayout(domain.laload(index, arrayref) :: rest, locals)
            }
            case 80 /*lastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.lastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            case 53 /*saload*/ ⇒ {
                val index :: arrayref :: rest = operands
                new MemoryLayout(domain.saload(index, arrayref) :: rest, locals)
            }
            case 86 /*sastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.sastore(value, index, arrayref)
                new MemoryLayout(rest, locals)
            }

            //
            // LENGTH OF AN ARRAY
            //

            case 190 /*arraylength*/ ⇒ {
                val arrayref = operands.head
                val newOperands = domain.arraylength(arrayref) :: operands.tail
                new MemoryLayout(newOperands, locals)
            }

            //
            // PUSH CONSTANT VALUE
            //

            case 1 /*aconst_null*/ ⇒
                new MemoryLayout(domain.nullValue :: operands, locals)

            case 16 /*bipush*/ ⇒
                new MemoryLayout(domain.byteValue(instruction.asInstanceOf[BIPUSH].value) :: operands, locals)

            case 14 /*dconst_0*/ ⇒ new MemoryLayout(domain.doubleValue(0.0d) :: operands, locals)
            case 15 /*dconst_1*/ ⇒ new MemoryLayout(domain.doubleValue(1.0d) :: operands, locals)

            case 11 /*fconst_0*/ ⇒ new MemoryLayout(domain.floatValue(0.0f) :: operands, locals)
            case 12 /*fconst_1*/ ⇒ new MemoryLayout(domain.floatValue(1.0f) :: operands, locals)
            case 13 /*fconst_2*/ ⇒ new MemoryLayout(domain.floatValue(2.0f) :: operands, locals)

            case 2 /*iconst_m1*/ ⇒ new MemoryLayout(domain.intValue(-1) :: operands, locals)
            case 3 /*iconst_0*/  ⇒ new MemoryLayout(domain.intValue(0) :: operands, locals)
            case 4 /*iconst_1*/  ⇒ new MemoryLayout(domain.intValue(1) :: operands, locals)
            case 5 /*iconst_2*/  ⇒ new MemoryLayout(domain.intValue(2) :: operands, locals)
            case 6 /*iconst_3*/  ⇒ new MemoryLayout(domain.intValue(3) :: operands, locals)
            case 7 /*iconst_4*/  ⇒ new MemoryLayout(domain.intValue(4) :: operands, locals)
            case 8 /*iconst_5*/  ⇒ new MemoryLayout(domain.intValue(5) :: operands, locals)

            case 9 /*lconst_0*/  ⇒ new MemoryLayout(domain.longValue(0l) :: operands, locals)
            case 10 /*lconst_1*/ ⇒ new MemoryLayout(domain.longValue(1l) :: operands, locals)

            case 18 /*ldc*/ ⇒ {
                instruction match {
                    case LoadInt(v)    ⇒ new MemoryLayout(domain.intValue(v) :: operands, locals)
                    case LoadFloat(v)  ⇒ new MemoryLayout(domain.floatValue(v) :: operands, locals)
                    case LoadString(v) ⇒ new MemoryLayout(domain.stringValue(v) :: operands, locals)
                    case LoadClass(v)  ⇒ new MemoryLayout(domain.classValue(v) :: operands, locals)
                    case _             ⇒ sys.error("internal implementation error or invalid bytecode")
                }
            }
            case 19 /*ldc_w*/ ⇒ {
                instruction match {
                    case LoadInt(v)    ⇒ new MemoryLayout(domain.intValue(v) :: operands, locals)
                    case LoadFloat(v)  ⇒ new MemoryLayout(domain.floatValue(v) :: operands, locals)
                    case LoadString(v) ⇒ new MemoryLayout(domain.stringValue(v) :: operands, locals)
                    case LoadClass(v)  ⇒ new MemoryLayout(domain.classValue(v) :: operands, locals)
                    case _             ⇒ sys.error("internal implementation error or invalid bytecode")
                }
            }
            case 20 /*ldc2_w*/ ⇒ {
                instruction match {
                    case LoadLong(v)   ⇒ new MemoryLayout(domain.longValue(v) :: operands, locals)
                    case LoadDouble(v) ⇒ new MemoryLayout(domain.doubleValue(v) :: operands, locals)
                    case _             ⇒ sys.error("internal implementation error or invalid bytecode")
                }
            }

            case 17 /*sipush*/ ⇒
                new MemoryLayout(domain.shortValue(instruction.asInstanceOf[SIPUSH].value) :: operands, locals)

            //
            // TYPE CHECKS AND CONVERSION
            //

            case 192 /*checkcast*/ ⇒ {
                val objectref :: rest = operands
                val newOperands = domain.checkcast(objectref, instruction.asInstanceOf[CHECKCAST].referenceType) :: rest
                new MemoryLayout(newOperands, locals)
            }
            case 193 /*instanceof*/ ⇒ {
                val objectref :: rest = operands
                val newOperands = domain.instanceof(objectref, instruction.asInstanceOf[INSTANCEOF].referenceType) :: rest
                new MemoryLayout(newOperands, locals)
            }

            case 144 /*d2f*/ ⇒ new MemoryLayout(domain.d2f(operands.head) :: (operands.tail), locals)
            case 142 /*d2i*/ ⇒ new MemoryLayout(domain.d2i(operands.head) :: (operands.tail), locals)
            case 143 /*d2l*/ ⇒ new MemoryLayout(domain.d2l(operands.head) :: (operands.tail), locals)

            case 141 /*f2d*/ ⇒ new MemoryLayout(domain.f2d(operands.head) :: (operands.tail), locals)
            case 139 /*f2i*/ ⇒ new MemoryLayout(domain.f2i(operands.head) :: (operands.tail), locals)
            case 140 /*f2l*/ ⇒ new MemoryLayout(domain.f2l(operands.head) :: (operands.tail), locals)

            case 145 /*i2b*/ ⇒ new MemoryLayout(domain.i2b(operands.head) :: (operands.tail), locals)
            case 146 /*i2c*/ ⇒ new MemoryLayout(domain.i2c(operands.head) :: (operands.tail), locals)
            case 135 /*i2d*/ ⇒ new MemoryLayout(domain.i2d(operands.head) :: (operands.tail), locals)
            case 134 /*i2f*/ ⇒ new MemoryLayout(domain.i2f(operands.head) :: (operands.tail), locals)
            case 133 /*i2l*/ ⇒ new MemoryLayout(domain.i2l(operands.head) :: (operands.tail), locals)
            case 147 /*i2s*/ ⇒ new MemoryLayout(domain.i2s(operands.head) :: (operands.tail), locals)

            case 138 /*l2d*/ ⇒ new MemoryLayout(domain.l2d(operands.head) :: (operands.tail), locals)
            case 137 /*l2f*/ ⇒ new MemoryLayout(domain.l2f(operands.head) :: (operands.tail), locals)
            case 136 /*l2i*/ ⇒ new MemoryLayout(domain.l2i(operands.head) :: (operands.tail), locals)

            //
            // RETURN FROM METHOD
            //
            case 176 /*areturn*/ ⇒ {
                domain.areturn(operands.head)
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 175 /*dreturn*/ ⇒ {
                domain.dreturn(operands.head)
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 174 /*freturn*/ ⇒ {
                domain.freturn(operands.head)
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 172 /*ireturn*/ ⇒ {
                domain.ireturn(operands.head)
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 173 /*lreturn*/ ⇒ {
                domain.lreturn(operands.head)
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 177 /*return*/ ⇒ {
                domain.returnVoid()
                new MemoryLayout(List.empty, IndexedSeq.empty)
            }
            
            //
            // THROWING EXCEPTION
            //
            
            case 191 /*athrow*/ ⇒
                new MemoryLayout(List(domain.athrow(operands.head)), locals)

            //
            // UNCONDITIONAL TRANSFER OF CONTROL
            //
            case 167 /*goto*/   ⇒ this
            case 200 /*goto_w*/ ⇒ this

            case 169 /*ret*/    ⇒ this
            case 168 /*jsr*/ ⇒
                new MemoryLayout(ReturnAddressValue(currentPC + 3) :: operands, locals)
            case 201 /*jsr_w*/ ⇒
                new MemoryLayout(ReturnAddressValue(currentPC + 5) :: operands, locals)

            //
            // CONDITIONAL TRANSFER OF CONTROL
            //
            case 171 /*lookupswitch*/
                | 170 /*tableswitch*/ ⇒ new MemoryLayout(operands.tail, locals)

            case 165 /*if_acmpeq*/
                | 166 /*if_acmpne*/
                | 159 /*if_icmpeq*/
                | 160 /*if_icmpne*/
                | 161 /*if_icmplt*/
                | 162 /*if_icmpge*/
                | 163 /*if_icmpgt*/
                | 164 /*if_icmple*/ ⇒ new MemoryLayout(operands.drop(2), locals)
            case 153 /*ifeq*/
                | 154 /*ifne*/
                | 155 /*iflt*/
                | 156 /*ifge*/
                | 157 /*ifgt*/
                | 158 /*ifle*/
                | 199 /*ifnonnull*/
                | 198 /*ifnull*/ ⇒ new MemoryLayout(operands.tail, locals)

            //
            // GENERIC STACK MANIPULATION
            //
            case 89 /*dup*/ ⇒
                new MemoryLayout((operands.head) :: operands, locals)
            case 90 /*dup_x1*/ ⇒ operands match {
                case v1 :: v2 :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 91 /*dup_x2*/ ⇒ operands match {
                case (v1 /*: CTC1*/ ) :: (v2: CTC1) :: (v3 /*: CTC1*/ ) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
                case (v1 /*: CTC1*/ ) :: v2 /* : CTC2*/ :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 92 /*dup2*/ ⇒ operands match {
                case (v1: CTC1) :: (v2 /*: CTC1*/ ) :: _ ⇒
                    new MemoryLayout(v1 :: v2 :: operands, locals)
                case (v /*: CTC2*/ ) :: _ ⇒
                    new MemoryLayout(v :: operands, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 93 /*dup2_x1*/ ⇒ operands match {
                case (v1: CTC1) :: (v2 /*: CTC1*/ ) :: (v3 /*: CTC1*/ ) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
                case (v1: CTC2) :: (v2 /*: CTC1*/ ) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 94 /*dup2_x2*/ ⇒ operands match {
                case (v1: CTC1) :: (v2: CTC1) :: (v3: CTC1) :: (v4 /*: CTC1*/ ) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest, locals)
                case (v1: CTC2) :: (v2: CTC1) :: (v3: CTC1) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
                case (v1: CTC1) :: (v2: CTC1) :: (v3: CTC2) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
                case (v1: CTC2) :: (v2 /*: CTC1*/ ) :: rest ⇒
                    new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }

            case 87 /*pop*/ ⇒
                new MemoryLayout(operands.tail, locals)
            case 88 /*pop2*/ ⇒
                operands.head match {
                    case _: CTC1 ⇒ new MemoryLayout(operands.drop(2), locals)
                    case _: CTC2 ⇒ new MemoryLayout(operands.tail, locals)
                }

            case 95 /*swap*/ ⇒ {
                val v1 :: v2 :: rest = operands
                new MemoryLayout(v2 :: v1 :: rest, locals)
            }

            //
            // ACCESSING FIELDS
            //
            case 180 /*getfield*/ ⇒ {
                val getfield = instruction.asInstanceOf[GETFIELD]
                new MemoryLayout(
                    domain.getfield(
                        operands.head,
                        getfield.declaringClass,
                        getfield.name,
                        getfield.fieldType) :: (operands.tail),
                    locals)
            }
            case 178 /*getstatic*/ ⇒ {
                val getstatic = instruction.asInstanceOf[GETSTATIC]
                new MemoryLayout(
                    domain.getstatic(
                        getstatic.declaringClass,
                        getstatic.name,
                        getstatic.fieldType) :: operands,
                    locals)
            }
            case 181 /*putfield*/ ⇒ {
                val putfield = instruction.asInstanceOf[PUTFIELD]
                val value :: objectref :: rest = operands
                domain.putfield(
                    objectref,
                    value,
                    putfield.declaringClass,
                    putfield.name,
                    putfield.fieldType)
                new MemoryLayout(rest, locals)
            }
            case 179 /*putstatic*/ ⇒ {
                val putstatic = instruction.asInstanceOf[PUTSTATIC]
                val value :: rest = operands
                domain.putstatic(
                    value,
                    putstatic.declaringClass,
                    putstatic.name,
                    putstatic.fieldType)
                new MemoryLayout(rest, locals)
            }

            //
            // METHOD INVOCATIONS
            //
            case 186 /*invokedynamic*/ ⇒ sys.error("invokedynamic is not yet supported")
            case 185 /*invokeinterface*/ ⇒ {
                val invoke = instruction.asInstanceOf[INVOKEINTERFACE]
                val argsCount = invoke.methodDescriptor.parameterTypes.length
                domain.invokeinterface(
                    invoke.declaringClass,
                    invoke.name,
                    invoke.methodDescriptor,
                    operands.take(argsCount + 1).reverse) match {
                        case Some(v) ⇒ new MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ new MemoryLayout(operands.drop(argsCount + 1), locals)
                    }
            }
            case 183 /*invokespecial*/ ⇒ {
                val invoke = instruction.asInstanceOf[INVOKESPECIAL]
                val argsCount = invoke.methodDescriptor.parameterTypes.length
                domain.invokespecial(
                    invoke.declaringClass,
                    invoke.name,
                    invoke.methodDescriptor,
                    operands.take(argsCount + 1).reverse) match {
                        case Some(v) ⇒ new MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ new MemoryLayout(operands.drop(argsCount + 1), locals)
                    }
            }
            case 184 /*invokestatic*/ ⇒ {
                val invoke = instruction.asInstanceOf[INVOKESTATIC]
                val argsCount = invoke.methodDescriptor.parameterTypes.length
                domain.invokestatic(
                    invoke.declaringClass,
                    invoke.name,
                    invoke.methodDescriptor,
                    operands.take(argsCount)) match {
                        case Some(v) ⇒ new MemoryLayout(v :: (operands.drop(argsCount)), locals)
                        case None    ⇒ new MemoryLayout(operands.drop(argsCount), locals)
                    }
            }
            case 182 /*invokevirtual*/ ⇒ {
                val invoke = instruction.asInstanceOf[INVOKEVIRTUAL]
                val argsCount = invoke.methodDescriptor.parameterTypes.length
                domain.invokevirtual(
                    invoke.declaringClass,
                    invoke.name,
                    invoke.methodDescriptor,
                    operands.take(argsCount + 1)) match {
                        case Some(v) ⇒ new MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ new MemoryLayout(operands.drop(argsCount + 1), locals)
                    }
            }

            //
            // RELATIONAL OPERATORS
            //
            case 150 /*fcmpg*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fcmpg(value1, value2) :: rest, locals)
            }
            case 149 /*fcmpl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fcmpl(value1, value2) :: rest, locals)
            }
            case 152 /*dcmpg*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.dcmpg(value1, value2) :: rest, locals)
            }
            case 151 /*dcmpl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.dcmpl(value1, value2) :: rest, locals)
            }
            case 148 /*lcmp*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lcmp(value1, value2) :: rest, locals)
            }

            //
            // UNARY EXPRESSIONS
            //
            case 119 /*dneg*/ ⇒
                new MemoryLayout(domain.dneg(operands.head) :: (operands.tail), locals)
            case 118 /*fneg*/ ⇒
                new MemoryLayout(domain.fneg(operands.head) :: (operands.tail), locals)
            case 117 /*lneg*/ ⇒
                new MemoryLayout(domain.lneg(operands.head) :: (operands.tail), locals)
            case 116 /*ineg*/ ⇒
                new MemoryLayout(domain.ineg(operands.head) :: (operands.tail), locals)

            //
            // BINARY EXPRESSIONS
            //

            case 99 /*dadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.dadd(value1, value2) :: rest, locals)
            }
            case 111 /*ddiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ddiv(value1, value2) :: rest, locals)
            }
            case 107 /*dmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.dmul(value1, value2) :: rest, locals)
            }
            case 115 /*drem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.drem(value1, value2) :: rest, locals)
            }
            case 103 /*dsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.dsub(value1, value2) :: rest, locals)
            }

            case 98 /*fadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fadd(value1, value2) :: rest, locals)
            }
            case 110 /*fdiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fdiv(value1, value2) :: rest, locals)
            }
            case 106 /*fmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fmul(value1, value2) :: rest, locals)
            }
            case 114 /*frem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.frem(value1, value2) :: rest, locals)
            }
            case 102 /*fsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.fsub(value1, value2) :: rest, locals)
            }

            case 96 /*iadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.iadd(value1, value2) :: rest, locals)
            }
            case 126 /*iand*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.iand(value1, value2) :: rest, locals)
            }
            case 108 /*idiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.idiv(value1, value2) :: rest, locals)
            }
            case 104 /*imul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.imul(value1, value2) :: rest, locals)
            }
            case 128 /*ior*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ior(value1, value2) :: rest, locals)
            }
            case 112 /*irem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.irem(value1, value2) :: rest, locals)
            }
            case 120 /*ishl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ishl(value1, value2) :: rest, locals)
            }
            case 122 /*ishr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ishr(value1, value2) :: rest, locals)
            }
            case 100 /*isub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.isub(value1, value2) :: rest, locals)
            }
            case 124 /*iushr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.iushr(value1, value2) :: rest, locals)
            }
            case 130 /*ixor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ixor(value1, value2) :: rest, locals)
            }

            case 97 /*ladd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ladd(value1, value2) :: rest, locals)
            }
            case 127 /*land*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.land(value1, value2) :: rest, locals)
            }
            case 109 /*ldiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.ldiv(value1, value2) :: rest, locals)
            }
            case 105 /*lmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lmul(value1, value2) :: rest, locals)
            }
            case 129 /*lor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lor(value1, value2) :: rest, locals)
            }
            case 113 /*lrem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lrem(value1, value2) :: rest, locals)
            }
            case 121 /*lshl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lshl(value1, value2) :: rest, locals)
            }
            case 123 /*lshr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lshr(value1, value2) :: rest, locals)
            }
            case 101 /*lsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lsub(value1, value2) :: rest, locals)
            }
            case 125 /*lushr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lushr(value1, value2) :: rest, locals)
            }
            case 131 /*lxor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                new MemoryLayout(domain.lxor(value1, value2) :: rest, locals)
            }

            //
            // "OTHER" INSTRUCTIONS
            //
            case 132 /*iinc*/ ⇒ {
                val iinc = instruction.asInstanceOf[IINC]
                val newValue = domain.iinc(locals(iinc.lvIndex), iinc.constValue)
                new MemoryLayout(operands, locals.updated(iinc.lvIndex, newValue))
            }

            case 194 /*monitorenter*/ ⇒ {
                domain.monitorenter(operands.head)
                new MemoryLayout(operands.tail, locals)
            }
            case 195 /*monitorexit*/ ⇒ {
                domain.monitorexit(operands.head)
                new MemoryLayout(operands.tail, locals)
            }

            case 187 /*new*/ ⇒ {
                val newObject = instruction.asInstanceOf[NEW]
                new MemoryLayout(domain.newObject(newObject.objectType) :: operands, locals)
            }

            case 0 /*nop*/    ⇒ this

            case 196 /*wide*/ ⇒ this // the instructions which are modified by a wide instruction already take care of the effect of wide
        }
    }
}

object MemoryLayout {

    /**
     * ==Note==
     * If you use this method to copy values from the stack into local variables, make
     * sure that you first reverse the order of operands.
     */
    def mapToLocals[V <: Value](params: List[V], locals: IndexedSeq[V]): IndexedSeq[V] = {
        var index = 0
        var initializedLocals = locals
        for (param ← params) {
            initializedLocals.updated(index, param)
            index += param.computationalType.operandSize
        }
        initializedLocals
    }
}