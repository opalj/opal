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
 * Represents the current execution context of a method. I.e., the operand stack
 * as well as the current values of the registers/locals. The memory layout is
 * automatically maintained by BATAI while analyzing a method. If specific knowledge
 * about a value is required, the domain is queried to get the necessary information.
 * This callback mechanism enables the domain to use an arbitrary mechanism to
 * represent values and to steer the analysis.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
/* The following design is the result of a lack of a feature for "constructor 
 * dependent types". If we would be able to express that the objects that are 
 * stored in the operands and locals lists are belonging to the given domain 
 * (and not just "some" domain, it would be possible to move the methods defined
 * in the companion object to this class.
 */
protected[ai] final class MemoryLayout[D <: Domain, V <: D#DomainValue](
        val domain: D,
        val operands: List[V],
        val locals: IndexedSeq[V]) {

    override def toString: String = {
        "MemoryLayout[(domain="+domain.getClass.getName+")\n"+
            operands.mkString("\toperands:", ", ", "\n") +
            locals.zipWithIndex.map(l ⇒ l._2+" -> "+l._1).mkString("\tlocals:\n\t\t", "\n\t\t", "\n")+
            "]"
    }
}

/**
 * Methods to create and update memory layouts.
 */
object MemoryLayout {

    /**
     * Updates this memory layout with the given memory layout. Returns `None` if this
     * memory layout already subsumes the given memory layout and returns
     * `Some(new MemoryLayout(...))` otherwise.
     */
    def merge(domain: Domain)(
        thisOperands: List[domain.DomainValue],
        thisLocals: IndexedSeq[domain.DomainValue],
        otherOperands: List[domain.DomainValue],
        otherLocals: IndexedSeq[domain.DomainValue]): Update[MemoryLayout[domain.type, domain.DomainValue]] = {

        assume(thisOperands.size == otherOperands.size,
            "merging the memory layouts is not possible due to different stack sizes")
        assume(thisLocals.size == otherLocals.size,
            "merging the memory layouts is not possible due to different numbers of locals")

        import domain.DomainValueTag

        var updateType: UpdateType = NoUpdateType

        var thisRemainingOperands = thisOperands
        var otherRemainingOperands = otherOperands
        var newOperands = List[domain.DomainValue]() // during the update we build the operands stack in reverse order

        while (thisRemainingOperands.nonEmpty /* the number of operands of both memory layouts is equal */ ) {
            val thisOperand = thisRemainingOperands.head
            val otherOperand = otherRemainingOperands.head
            otherRemainingOperands = otherRemainingOperands.tail
            thisRemainingOperands = thisRemainingOperands.tail

            val updatedOperand = thisOperand merge otherOperand
            val newOperand = updatedOperand match {
                case SomeUpdate(operand) ⇒ operand
                case NoUpdate            ⇒ thisOperand
            }
            assume(!newOperand.isInstanceOf[Domain#NoLegalValue], "merging of stack values ("+thisOperand+" and "+otherOperand+") led to an illegal value")
            updateType = updateType &: updatedOperand
            newOperands = newOperand :: newOperands
        }

        val maxLocals = thisLocals.size

        val newLocals = new Array[domain.DomainValue](maxLocals)
        var i = 0;
        while (i < maxLocals) {
            val thisLocal = thisLocals(i)
            val otherLocal = otherLocals(i)
            // The value calculated by "merge" may be the value "NoLegalValue" which means
            // the values in the corresponding register were different (path dependent)
            // on the different paths.
            // If we would have a liveness analysis, we could avoid the use of 
            // "NoLegalValue"
            val newLocal =
                if ((thisLocal eq null) || (otherLocal eq null)) {
                    updateType = updateType &: MetaInformationUpdateType
                    domain.NoLegalValue("a register/local did not contain any value")
                } else {
                    val updatedLocal = thisLocal merge otherLocal
                    updateType = updateType &: updatedLocal
                    updatedLocal match {
                        case SomeUpdate(operand) ⇒ operand
                        case NoUpdate            ⇒ thisLocal
                    }

                }
            newLocals(i) = newLocal
            i += 1
        }

        updateType(new MemoryLayout(domain, newOperands.reverse, newLocals))
    }

    def update(
        domain: Domain)(
            operands: List[domain.DomainValue],
            locals: IndexedSeq[domain.DomainValue],
            currentPC: Int,
            instruction: Instruction): MemoryLayout[domain.type, domain.DomainValue] = {

        import domain.CTC1
        import domain.CTC2

        def MemoryLayout(
            operands: List[domain.DomainValue],
            locals: IndexedSeq[domain.DomainValue]): MemoryLayout[domain.type, domain.DomainValue] =
            new MemoryLayout(domain, operands, locals)

        def self(): MemoryLayout[domain.type, domain.DomainValue] =
            new MemoryLayout(domain, operands, locals)

        (instruction.opcode: @annotation.switch) match {

            //
            // PUT LOCAL VARIABLE VALUE ONTO STACK
            //
            case 25 /*aload*/ ⇒
                MemoryLayout(locals(instruction.asInstanceOf[ALOAD].lvIndex) :: operands, locals)
            case 24 /*dload*/ ⇒
                MemoryLayout(locals(instruction.asInstanceOf[DLOAD].lvIndex) :: operands, locals)
            case 23 /*fload*/ ⇒
                MemoryLayout(locals(instruction.asInstanceOf[FLOAD].lvIndex) :: operands, locals)
            case 21 /*iload*/ ⇒
                MemoryLayout(locals(instruction.asInstanceOf[ILOAD].lvIndex) :: operands, locals)
            case 22 /*lload*/ ⇒
                MemoryLayout(locals(instruction.asInstanceOf[LLOAD].lvIndex) :: operands, locals)
            case 42 /*aload_0*/ | 38 /*dload_0*/ | 34 /*fload_0*/ | 26 /*iload_0*/ | 30 /*lload_0*/ ⇒
                MemoryLayout(locals(0) :: operands, locals)
            case 43 /*aload_1*/ | 39 /*dload_1*/ | 35 /*fload_1*/ | 27 /*iload_1*/ | 31 /*lload_1*/ ⇒
                MemoryLayout(locals(1) :: operands, locals)
            case 44 /*aload_2*/ | 40 /*dload_2*/ | 36 /*fload_2*/ | 28 /*iload_2*/ | 32 /*lload_2*/ ⇒
                MemoryLayout(locals(2) :: operands, locals)
            case 45 /*aload_3*/ | 41 /*dload_3*/ | 37 /*fload_3*/ | 29 /*iload_3*/ | 33 /*lload_3*/ ⇒
                MemoryLayout(locals(3) :: operands, locals)

            //
            // STORE OPERAND IN LOCAL VARIABLE
            //
            case 58 /*astore*/ ⇒
                MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[ASTORE].lvIndex, operands.head))
            case 57 /*dstore*/ ⇒
                MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[DSTORE].lvIndex, operands.head))
            case 56 /*fstore*/ ⇒
                MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[FSTORE].lvIndex, operands.head))
            case 54 /*istore*/ ⇒
                MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[ISTORE].lvIndex, operands.head))
            case 55 /*lstore*/ ⇒
                MemoryLayout(operands.tail,
                    locals.updated(instruction.asInstanceOf[LSTORE].lvIndex, operands.head))
            case 75 /*astore_0*/
                | 71 /*dstore_0*/
                | 67 /*fstore_0*/
                | 63 /*lstore_0*/
                | 59 /*istore_0*/ ⇒
                MemoryLayout(operands.tail, locals.updated(0, operands.head))
            case 76 /*astore_1*/
                | 72 /*dstore_1*/
                | 68 /*fstore_1*/
                | 64 /*lstore_1*/
                | 60 /*istore_1*/ ⇒
                MemoryLayout(operands.tail, locals.updated(1, operands.head))
            case 77 /*astore_2*/
                | 73 /*dstore_2*/
                | 69 /*fstore_2*/
                | 65 /*lstore_2*/
                | 61 /*istore_2*/ ⇒
                MemoryLayout(operands.tail, locals.updated(2, operands.head))
            case 78 /*astore_3*/
                | 74 /*dstore_3*/
                | 70 /*fstore_3*/
                | 66 /*lstore_3*/
                | 62 /*istore_3*/ ⇒
                MemoryLayout(operands.tail, locals.updated(3, operands.head))

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
                MemoryLayout(newOperands, locals)
            }
            case 189 /*anewarray*/ ⇒ {
                val count :: rest = operands
                val newOperands = domain.newarray(count, instruction.asInstanceOf[ANEWARRAY].componentType) :: rest
                MemoryLayout(newOperands, locals)
            }

            case 197 /*multianewarray*/ ⇒ {
                val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                val initDimensions = operands.take(multianewarray.dimensions)
                MemoryLayout(
                    domain.multianewarray(initDimensions, multianewarray.componentType) :: (operands.drop(multianewarray.dimensions)),
                    locals)
            }

            //
            // LOAD FROM AND STORE VALUE IN ARRAYS
            //

            case 50 /*aaload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.aaload(index, arrayref) :: rest
                MemoryLayout(newOperands, locals)
            }
            case 83 /*aastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.aastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 51 /*baload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.baload(index, arrayref) :: rest
                MemoryLayout(newOperands, locals)
            }
            case 84 /*bastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.bastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 52 /*caload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.caload(index, arrayref) :: rest
                MemoryLayout(newOperands, locals)
            }
            case 85 /*castore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.castore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 49 /*daload*/ ⇒ {
                val index :: arrayref :: rest = operands
                val newOperands = domain.daload(index, arrayref) :: rest
                MemoryLayout(newOperands, locals)
            }
            case 82 /*dastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.dastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 48 /*faload*/ ⇒ {
                val index :: arrayref :: rest = operands
                MemoryLayout(domain.faload(index, arrayref) :: rest, locals)
            }
            case 81 /*fastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.fastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 46 /*iaload*/ ⇒ {
                val index :: arrayref :: rest = operands
                MemoryLayout(domain.iaload(index, arrayref) :: rest, locals)
            }
            case 79 /*iastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.iastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 47 /*laload*/ ⇒ {
                val index :: arrayref :: rest = operands
                MemoryLayout(domain.laload(index, arrayref) :: rest, locals)
            }
            case 80 /*lastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.lastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            case 53 /*saload*/ ⇒ {
                val index :: arrayref :: rest = operands
                MemoryLayout(domain.saload(index, arrayref) :: rest, locals)
            }
            case 86 /*sastore*/ ⇒ {
                val value :: index :: arrayref :: rest = operands
                domain.sastore(value, index, arrayref)
                MemoryLayout(rest, locals)
            }

            //
            // LENGTH OF AN ARRAY
            //

            case 190 /*arraylength*/ ⇒ {
                val arrayref = operands.head
                val newOperands = domain.arraylength(arrayref) :: operands.tail
                MemoryLayout(newOperands, locals)
            }

            //
            // PUSH CONSTANT VALUE
            //

            case 1 /*aconst_null*/ ⇒
                MemoryLayout(domain.nullValue :: operands, locals)

            case 16 /*bipush*/ ⇒
                MemoryLayout(domain.byteValue(instruction.asInstanceOf[BIPUSH].value) :: operands, locals)

            case 14 /*dconst_0*/ ⇒ MemoryLayout(domain.doubleValue(0.0d) :: operands, locals)
            case 15 /*dconst_1*/ ⇒ MemoryLayout(domain.doubleValue(1.0d) :: operands, locals)

            case 11 /*fconst_0*/ ⇒ MemoryLayout(domain.floatValue(0.0f) :: operands, locals)
            case 12 /*fconst_1*/ ⇒ MemoryLayout(domain.floatValue(1.0f) :: operands, locals)
            case 13 /*fconst_2*/ ⇒ MemoryLayout(domain.floatValue(2.0f) :: operands, locals)

            case 2 /*iconst_m1*/ ⇒ MemoryLayout(domain.intValue(-1) :: operands, locals)
            case 3 /*iconst_0*/  ⇒ MemoryLayout(domain.intValue(0) :: operands, locals)
            case 4 /*iconst_1*/  ⇒ MemoryLayout(domain.intValue(1) :: operands, locals)
            case 5 /*iconst_2*/  ⇒ MemoryLayout(domain.intValue(2) :: operands, locals)
            case 6 /*iconst_3*/  ⇒ MemoryLayout(domain.intValue(3) :: operands, locals)
            case 7 /*iconst_4*/  ⇒ MemoryLayout(domain.intValue(4) :: operands, locals)
            case 8 /*iconst_5*/  ⇒ MemoryLayout(domain.intValue(5) :: operands, locals)

            case 9 /*lconst_0*/  ⇒ MemoryLayout(domain.longValue(0l) :: operands, locals)
            case 10 /*lconst_1*/ ⇒ MemoryLayout(domain.longValue(1l) :: operands, locals)

            case 18 /*ldc*/ ⇒ {
                instruction match {
                    case LoadInt(v)    ⇒ MemoryLayout(domain.intValue(v) :: operands, locals)
                    case LoadFloat(v)  ⇒ MemoryLayout(domain.floatValue(v) :: operands, locals)
                    case LoadString(v) ⇒ MemoryLayout(domain.stringValue(v) :: operands, locals)
                    case LoadClass(v)  ⇒ MemoryLayout(domain.classValue(v) :: operands, locals)
                    case _             ⇒ BATError("internal implementation error or invalid bytecode")
                }
            }
            case 19 /*ldc_w*/ ⇒ {
                instruction match {
                    case LoadInt_W(v)    ⇒ MemoryLayout(domain.intValue(v) :: operands, locals)
                    case LoadFloat_W(v)  ⇒ MemoryLayout(domain.floatValue(v) :: operands, locals)
                    case LoadString_W(v) ⇒ MemoryLayout(domain.stringValue(v) :: operands, locals)
                    case LoadClass_W(v)  ⇒ MemoryLayout(domain.classValue(v) :: operands, locals)
                    case _               ⇒ BATError("internal implementation error or invalid bytecode")
                }
            }
            case 20 /*ldc2_w*/ ⇒ {
                instruction match {
                    case LoadLong(v)   ⇒ MemoryLayout(domain.longValue(v) :: operands, locals)
                    case LoadDouble(v) ⇒ MemoryLayout(domain.doubleValue(v) :: operands, locals)
                    case _             ⇒ BATError("internal implementation error or invalid bytecode")
                }
            }

            case 17 /*sipush*/ ⇒
                MemoryLayout(domain.shortValue(instruction.asInstanceOf[SIPUSH].value) :: operands, locals)

            //
            // TYPE CHECKS AND CONVERSION
            //

            case 192 /*checkcast*/ ⇒ {
                val objectref :: rest = operands
                val newOperands = domain.checkcast(objectref, instruction.asInstanceOf[CHECKCAST].referenceType) :: rest
                MemoryLayout(newOperands, locals)
            }
            case 193 /*instanceof*/ ⇒ {
                val objectref :: rest = operands
                val newOperands = domain.instanceof(objectref, instruction.asInstanceOf[INSTANCEOF].referenceType) :: rest
                MemoryLayout(newOperands, locals)
            }

            case 144 /*d2f*/ ⇒ MemoryLayout(domain.d2f(operands.head) :: (operands.tail), locals)
            case 142 /*d2i*/ ⇒ MemoryLayout(domain.d2i(operands.head) :: (operands.tail), locals)
            case 143 /*d2l*/ ⇒ MemoryLayout(domain.d2l(operands.head) :: (operands.tail), locals)

            case 141 /*f2d*/ ⇒ MemoryLayout(domain.f2d(operands.head) :: (operands.tail), locals)
            case 139 /*f2i*/ ⇒ MemoryLayout(domain.f2i(operands.head) :: (operands.tail), locals)
            case 140 /*f2l*/ ⇒ MemoryLayout(domain.f2l(operands.head) :: (operands.tail), locals)

            case 145 /*i2b*/ ⇒ MemoryLayout(domain.i2b(operands.head) :: (operands.tail), locals)
            case 146 /*i2c*/ ⇒ MemoryLayout(domain.i2c(operands.head) :: (operands.tail), locals)
            case 135 /*i2d*/ ⇒ MemoryLayout(domain.i2d(operands.head) :: (operands.tail), locals)
            case 134 /*i2f*/ ⇒ MemoryLayout(domain.i2f(operands.head) :: (operands.tail), locals)
            case 133 /*i2l*/ ⇒ MemoryLayout(domain.i2l(operands.head) :: (operands.tail), locals)
            case 147 /*i2s*/ ⇒ MemoryLayout(domain.i2s(operands.head) :: (operands.tail), locals)

            case 138 /*l2d*/ ⇒ MemoryLayout(domain.l2d(operands.head) :: (operands.tail), locals)
            case 137 /*l2f*/ ⇒ MemoryLayout(domain.l2f(operands.head) :: (operands.tail), locals)
            case 136 /*l2i*/ ⇒ MemoryLayout(domain.l2i(operands.head) :: (operands.tail), locals)

            //
            // RETURN FROM METHOD
            //
            case 176 /*areturn*/ ⇒ {
                domain.areturn(operands.head)
                MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 175 /*dreturn*/ ⇒ {
                domain.dreturn(operands.head)
                MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 174 /*freturn*/ ⇒ {
                domain.freturn(operands.head)
                MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 172 /*ireturn*/ ⇒ {
                domain.ireturn(operands.head)
                MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 173 /*lreturn*/ ⇒ {
                domain.lreturn(operands.head)
                MemoryLayout(List.empty, IndexedSeq.empty)
            }
            case 177 /*return*/ ⇒ {
                domain.returnVoid()
                MemoryLayout(List.empty, IndexedSeq.empty)
            }

            //
            // THROWING EXCEPTION
            //

            case 191 /*athrow*/ ⇒
                MemoryLayout(List(domain.athrow(operands.head)), locals)

            //
            // UNCONDITIONAL TRANSFER OF CONTROL
            //
            case 167 /*goto*/   ⇒ self
            case 200 /*goto_w*/ ⇒ self

            case 169 /*ret*/    ⇒ self
            case 168 /*jsr*/ ⇒
                MemoryLayout(domain.ReturnAddressValue(currentPC + 3) :: operands, locals)
            case 201 /*jsr_w*/ ⇒
                MemoryLayout(domain.ReturnAddressValue(currentPC + 5) :: operands, locals)

            //
            // CONDITIONAL TRANSFER OF CONTROL
            //
            case 171 /*lookupswitch*/
                | 170 /*tableswitch*/ ⇒ MemoryLayout(operands.tail, locals)

            case 165 /*if_acmpeq*/
                | 166 /*if_acmpne*/
                | 159 /*if_icmpeq*/
                | 160 /*if_icmpne*/
                | 161 /*if_icmplt*/
                | 162 /*if_icmpge*/
                | 163 /*if_icmpgt*/
                | 164 /*if_icmple*/ ⇒ MemoryLayout(operands.drop(2), locals)
            case 153 /*ifeq*/
                | 154 /*ifne*/
                | 155 /*iflt*/
                | 156 /*ifge*/
                | 157 /*ifgt*/
                | 158 /*ifle*/
                | 199 /*ifnonnull*/
                | 198 /*ifnull*/ ⇒ MemoryLayout(operands.tail, locals)

            //
            // GENERIC STACK MANIPULATION
            //
            case 89 /*dup*/ ⇒
                MemoryLayout((operands.head) :: operands, locals)
            case 90 /*dup_x1*/ ⇒ operands match {
                case v1 :: v2 :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 91 /*dup_x2*/ ⇒ operands match {
                case (v1 /*@ CTC1()*/ ) :: (v2 @ CTC1()) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
                case (v1 /*@ CTC1()*/ ) :: v2 /* @ CTC2()*/ :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 92 /*dup2*/ ⇒ operands match {
                case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: _ ⇒
                    MemoryLayout(v1 :: v2 :: operands, locals)
                case (v /*@ CTC2()*/ ) :: _ ⇒
                    MemoryLayout(v :: operands, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 93 /*dup2_x1*/ ⇒ operands match {
                case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
                case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }
            case 94 /*dup2_x2*/ ⇒ operands match {
                case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: (v4 /*@ CTC1()*/ ) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest, locals)
                case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
                case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
                case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                    MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
                case _ ⇒ sys.error("internal implementation error or invalid bytecode")
            }

            case 87 /*pop*/ ⇒
                MemoryLayout(operands.tail, locals)
            case 88 /*pop2*/ ⇒
                operands.head match {
                    case _@ CTC1() ⇒ MemoryLayout(operands.drop(2), locals)
                    case _@ CTC2() ⇒ MemoryLayout(operands.tail, locals)
                }

            case 95 /*swap*/ ⇒ {
                val v1 :: v2 :: rest = operands
                MemoryLayout(v2 :: v1 :: rest, locals)
            }

            //
            // ACCESSING FIELDS
            //
            case 180 /*getfield*/ ⇒ {
                val getfield = instruction.asInstanceOf[GETFIELD]
                MemoryLayout(
                    domain.getfield(
                        operands.head,
                        getfield.declaringClass,
                        getfield.name,
                        getfield.fieldType) :: (operands.tail),
                    locals)
            }
            case 178 /*getstatic*/ ⇒ {
                val getstatic = instruction.asInstanceOf[GETSTATIC]
                MemoryLayout(
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
                MemoryLayout(rest, locals)
            }
            case 179 /*putstatic*/ ⇒ {
                val putstatic = instruction.asInstanceOf[PUTSTATIC]
                val value :: rest = operands
                domain.putstatic(
                    value,
                    putstatic.declaringClass,
                    putstatic.name,
                    putstatic.fieldType)
                MemoryLayout(rest, locals)
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
                        case Some(v) ⇒ MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ MemoryLayout(operands.drop(argsCount + 1), locals)
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
                        case Some(v) ⇒ MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ MemoryLayout(operands.drop(argsCount + 1), locals)
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
                        case Some(v) ⇒ MemoryLayout(v :: (operands.drop(argsCount)), locals)
                        case None    ⇒ MemoryLayout(operands.drop(argsCount), locals)
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
                        case Some(v) ⇒ MemoryLayout(v :: (operands.drop(argsCount + 1)), locals)
                        case None    ⇒ MemoryLayout(operands.drop(argsCount + 1), locals)
                    }
            }

            //
            // RELATIONAL OPERATORS
            //
            case 150 /*fcmpg*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fcmpg(value1, value2) :: rest, locals)
            }
            case 149 /*fcmpl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fcmpl(value1, value2) :: rest, locals)
            }
            case 152 /*dcmpg*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.dcmpg(value1, value2) :: rest, locals)
            }
            case 151 /*dcmpl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.dcmpl(value1, value2) :: rest, locals)
            }
            case 148 /*lcmp*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lcmp(value1, value2) :: rest, locals)
            }

            //
            // UNARY EXPRESSIONS
            //
            case 119 /*dneg*/ ⇒
                MemoryLayout(domain.dneg(operands.head) :: (operands.tail), locals)
            case 118 /*fneg*/ ⇒
                MemoryLayout(domain.fneg(operands.head) :: (operands.tail), locals)
            case 117 /*lneg*/ ⇒
                MemoryLayout(domain.lneg(operands.head) :: (operands.tail), locals)
            case 116 /*ineg*/ ⇒
                MemoryLayout(domain.ineg(operands.head) :: (operands.tail), locals)

            //
            // BINARY EXPRESSIONS
            //

            case 99 /*dadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.dadd(value1, value2) :: rest, locals)
            }
            case 111 /*ddiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ddiv(value1, value2) :: rest, locals)
            }
            case 107 /*dmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.dmul(value1, value2) :: rest, locals)
            }
            case 115 /*drem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.drem(value1, value2) :: rest, locals)
            }
            case 103 /*dsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.dsub(value1, value2) :: rest, locals)
            }

            case 98 /*fadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fadd(value1, value2) :: rest, locals)
            }
            case 110 /*fdiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fdiv(value1, value2) :: rest, locals)
            }
            case 106 /*fmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fmul(value1, value2) :: rest, locals)
            }
            case 114 /*frem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.frem(value1, value2) :: rest, locals)
            }
            case 102 /*fsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.fsub(value1, value2) :: rest, locals)
            }

            case 96 /*iadd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.iadd(value1, value2) :: rest, locals)
            }
            case 126 /*iand*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.iand(value1, value2) :: rest, locals)
            }
            case 108 /*idiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.idiv(value1, value2) :: rest, locals)
            }
            case 104 /*imul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.imul(value1, value2) :: rest, locals)
            }
            case 128 /*ior*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ior(value1, value2) :: rest, locals)
            }
            case 112 /*irem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.irem(value1, value2) :: rest, locals)
            }
            case 120 /*ishl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ishl(value1, value2) :: rest, locals)
            }
            case 122 /*ishr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ishr(value1, value2) :: rest, locals)
            }
            case 100 /*isub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.isub(value1, value2) :: rest, locals)
            }
            case 124 /*iushr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.iushr(value1, value2) :: rest, locals)
            }
            case 130 /*ixor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ixor(value1, value2) :: rest, locals)
            }

            case 97 /*ladd*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ladd(value1, value2) :: rest, locals)
            }
            case 127 /*land*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.land(value1, value2) :: rest, locals)
            }
            case 109 /*ldiv*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.ldiv(value1, value2) :: rest, locals)
            }
            case 105 /*lmul*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lmul(value1, value2) :: rest, locals)
            }
            case 129 /*lor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lor(value1, value2) :: rest, locals)
            }
            case 113 /*lrem*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lrem(value1, value2) :: rest, locals)
            }
            case 121 /*lshl*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lshl(value1, value2) :: rest, locals)
            }
            case 123 /*lshr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lshr(value1, value2) :: rest, locals)
            }
            case 101 /*lsub*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lsub(value1, value2) :: rest, locals)
            }
            case 125 /*lushr*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lushr(value1, value2) :: rest, locals)
            }
            case 131 /*lxor*/ ⇒ {
                val value2 :: value1 :: rest = operands
                MemoryLayout(domain.lxor(value1, value2) :: rest, locals)
            }

            //
            // "OTHER" INSTRUCTIONS
            //
            case 132 /*iinc*/ ⇒ {
                val iinc = instruction.asInstanceOf[IINC]
                val newValue = domain.iinc(locals(iinc.lvIndex), iinc.constValue)
                MemoryLayout(operands, locals.updated(iinc.lvIndex, newValue))
            }

            case 194 /*monitorenter*/ ⇒ {
                domain.monitorenter(operands.head)
                MemoryLayout(operands.tail, locals)
            }
            case 195 /*monitorexit*/ ⇒ {
                domain.monitorexit(operands.head)
                MemoryLayout(operands.tail, locals)
            }

            case 187 /*new*/ ⇒ {
                val newObject = instruction.asInstanceOf[NEW]
                MemoryLayout(domain.newObject(newObject.objectType) :: operands, locals)
            }

            case 0 /*nop*/    ⇒ self

            case 196 /*wide*/ ⇒ self // the instructions which are modified by a wide instruction already take care of the effect of wide
        }
    }

    /**
     * ==Note==
     * If you use this method to copy values from the stack into local variables, make
     * sure that you first reverse the order of operands.
     */
    def mapToLocals[V <: Domain#DomainValue](params: List[V], locals: IndexedSeq[V]): IndexedSeq[V] = {
        var index = 0
        var initializedLocals = locals
        for (param ← params) {
            initializedLocals.updated(index, param)
            index += param.computationalType.operandSize
        }
        initializedLocals
    }
}