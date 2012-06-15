package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

trait SomeMemoryLayout[+M <: SomeMemoryLayout[_]] {
	def locals : IndexedSeq[Value]
	def operands : List[Value]
	def update(instruction : BytecodeInstruction) : M
}

class MemoryLayout(
	val operands : List[Value],
	val locals : IndexedSeq[Value])(
		implicit domain : Domain)
	extends SomeMemoryLayout[MemoryLayout] {

	/**
	 * Extractor object that matches `Value`s which have computational type category 1.
	 */
	private[this] object CTC1 {
		def unapply(value : Value) : Boolean = value.computationalType.computationTypeCategory.id == 1
	}
	/**
	 * Extractor object that matches `Value`s which have computational type category 2.
	 */
	private[this] object CTC2 {
		def unapply(value : Value) : Boolean = value.computationalType.computationTypeCategory.id == 2
	}

	def update(instruction : BytecodeInstruction) : MemoryLayout = {
		import annotation.switch
		(instruction.opcode : @switch) match {
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
			case 1 /*aconst_null*/ ⇒
				new MemoryLayout(domain.aconstNull :: operands, locals)
			case 25 /*aload*/ ⇒ new MemoryLayout(locals(instruction.asInstanceOf[ALOAD].lvIndex) :: operands, locals)
			case 42 /*aload_0*/ ⇒ new MemoryLayout(locals(0) :: operands, locals)
			case 43 /*aload_1*/ ⇒ new MemoryLayout(locals(1) :: operands, locals)
			case 44 /*aload_2*/ ⇒ new MemoryLayout(locals(2) :: operands, locals)
			case 45 /*aload_3*/ ⇒ new MemoryLayout(locals(3) :: operands, locals)
			case 189 /*anewarray*/ ⇒ {
				val count :: rest = operands
				val newOperands = domain.anewarray(count, instruction.asInstanceOf[ANEWARRAY].componentType) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 176 /*areturn*/ ⇒ {
				domain.areturn(operands.head)
				new MemoryLayout(List.empty, IndexedSeq.empty)
			}
			case 190 /*arraylength*/ ⇒ {
				val arrayref = operands.head
				val newOperands = domain.arraylength(arrayref) :: operands.tail
				new MemoryLayout(newOperands, locals)
			}
			case 58 /*astore*/ ⇒ new MemoryLayout(operands.tail, locals.updated(instruction.asInstanceOf[ASTORE].lvIndex, operands.head))
			case 75 /*astore_0*/ ⇒ new MemoryLayout(operands.tail, locals.updated(0, operands.head))
			case 76 /*astore_1*/ ⇒ new MemoryLayout(operands.tail, locals.updated(1, operands.head))
			case 77 /*astore_2*/ ⇒ new MemoryLayout(operands.tail, locals.updated(2, operands.head))
			case 78 /*astore_3*/ ⇒ new MemoryLayout(operands.tail, locals.updated(3, operands.head))
			case 191 /*athrow*/ ⇒ new MemoryLayout(
				{
					val v = operands.head
					v match {
						case NullValue ⇒ List(TypedValue(InstructionExceptions.NullPointerException))
						case _ ⇒ List(v)
					}
				},
				locals)
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
			case 16 /*bipush*/ ⇒ new MemoryLayout(TypedValue.ByteValue :: (operands.tail), locals)
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
			case 192 /*checkcast*/ ⇒ {
				val objectref :: rest = operands
				val newOperands = domain.checkcast(objectref, instruction.asInstanceOf[CHECKCAST].referenceType) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 144 /*d2f*/ ⇒ new MemoryLayout(TypedValue.FloatValue :: (operands.tail), locals)
			case 142 /*d2i*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: (operands.tail), locals)
			case 143 /*d2l*/ ⇒ new MemoryLayout(TypedValue.LongValue :: (operands.tail), locals)
			case 99 /*dadd*/ ⇒ {
				val value2 :: value1 :: rest = operands
				val newOperands = domain.arithmeticExpression(DoubleType, Operator.Add, value2, value1) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 49 /*daload*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: (operands.tail.tail), locals)
			case 82 /*dastore*/ ⇒ new MemoryLayout(operands.tail.tail.tail, locals)

			case 152 /*dcmpg*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: (operands.tail.tail), locals)
			case 151 /*dcmpl*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: (operands.tail.tail), locals)
			case 14 /*dconst_0*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: operands, locals)
			case 15 /*dconst_1*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: operands, locals)
			case 111 /*ddiv*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 24 /*dload*/ ⇒ new MemoryLayout(locals(instruction.asInstanceOf[DLOAD].lvIndex) :: operands, locals)
			case 38 /*dload_0*/ ⇒ new MemoryLayout(locals(0) :: operands, locals)
			case 39 /*dload_1*/ ⇒ new MemoryLayout(locals(1) :: operands, locals)
			case 40 /*dload_2*/ ⇒ new MemoryLayout(locals(2) :: operands, locals)
			case 41 /*dload_3*/ ⇒ new MemoryLayout(locals(3) :: operands, locals)
			case 107 /*dmul*/ ⇒ {
				val value2 :: value1 :: rest = operands
				val newOperands = domain.arithmeticExpression(DoubleType, Operator.Mult, value2, value1) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 119 /*dneg*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: (operands.tail), locals)
			case 115 /*drem*/ ⇒ {
				val value2 :: value1 :: rest = operands
				val newOperands = domain.arithmeticExpression(DoubleType, Operator.Rem, value2, value1) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 175 /*dreturn*/ ⇒ {
				domain.dreturn(operands.head)
				new MemoryLayout(List.empty, IndexedSeq.empty)
			}
			case 57 /*dstore*/ ⇒ new MemoryLayout(operands.tail, locals.updated(instruction.asInstanceOf[DSTORE].lvIndex, operands.head))
			case 71 /*dstore_0*/ ⇒ new MemoryLayout(operands.tail, locals.updated(0, operands.head))
			case 72 /*dstore_1*/ ⇒ new MemoryLayout(operands.tail, locals.updated(1, operands.head))
			case 73 /*dstore_2*/ ⇒ new MemoryLayout(operands.tail, locals.updated(2, operands.head))
			case 74 /*dstore_3*/ ⇒ new MemoryLayout(operands.tail, locals.updated(3, operands.head))
			case 103 /*dsub*/ ⇒ {
				val value2 :: value1 :: rest = operands
				val newOperands = domain.arithmeticExpression(DoubleType, Operator.Sub, value2, value1) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 89 /*dup*/ ⇒
				new MemoryLayout((operands.head) :: operands, locals)
			case 90 /*dup_x1*/ ⇒ operands match {
				case v1 :: v2 :: rest ⇒
					new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
				case _ ⇒ sys.error("internal implementation error or invalid bytecode")
			}
			case 91 /*dup_x2*/ ⇒ operands match {
				case (v1 /*@ CTC1()*/ ) :: (v2 @ CTC1()) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
					new MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
				case (v1 /*@ CTC1()*/ ) :: v2 /* @ CTC2()*/ :: rest ⇒
					new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
				case _ ⇒ sys.error("internal implementation error or invalid bytecode")
			}
			case 92 /*dup2*/ ⇒ operands match {
				case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: _ ⇒
					new MemoryLayout(v1 :: v2 :: operands, locals)
				case (v /*@ CTC2()*/ ) :: _ ⇒
					new MemoryLayout(v :: operands, locals)
				case _ ⇒ sys.error("internal implementation error or invalid bytecode")
			}
			case 93 /*dup2_x1*/ ⇒ operands match {
				case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: (v3 /*@ CTC1()*/ ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
				case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
				case _ ⇒ sys.error("internal implementation error or invalid bytecode")
			}
			case 94 /*dup2_x2*/ ⇒ operands match {
				case (v1 @ CTC1()) :: (v2 @ CTC1() ) :: (v3 @ CTC1() ) :: (v4 /*@ CTC1()*/ ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest, locals)
				case (v1 @ CTC2()) :: (v2 @ CTC1() ) :: (v3 @ CTC1() ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v3 :: v1 :: rest, locals)
				case (v1 @ CTC1()) :: (v2 @ CTC1() ) :: (v3 @ CTC2() ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v3 :: v1 :: v2 :: rest, locals)
				case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest =>
					new MemoryLayout(v1 :: v2 :: v1 :: rest, locals)
				case _ ⇒ sys.error("internal implementation error or invalid bytecode")
			}
			case 141 /*f2d*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: (operands.tail), locals)
			case 139 /*f2i*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: (operands.tail), locals)
			case 140 /*f2l*/ ⇒ new MemoryLayout(TypedValue.LongValue :: (operands.tail), locals)
			case 98 /*fadd*/ ⇒ {
				val value2 :: value1 :: rest = operands
				val newOperands = domain.arithmeticExpression(FloatType, Operator.Add, value2, value1) :: rest
				new MemoryLayout(newOperands, locals)
			}
			case 48 /*faload*/ ⇒ new MemoryLayout(TypedValue.FloatValue :: (operands.tail.tail), locals)
			case 81 /*fastore*/ ⇒ new MemoryLayout(operands.tail.tail.tail, locals)
			case 150 /*fcmpg*/ ⇒ new MemoryLayout(TypedValue.FloatValue :: (operands.tail.tail), locals)
			case 149 /*fcmpl*/ ⇒ new MemoryLayout(TypedValue.FloatValue :: (operands.tail.tail), locals)
			case 11 /*fconst_0*/ ⇒ null
			case 12 /*fconst_1*/ ⇒ null
			case 13 /*fconst_2*/ ⇒ null
			case 110 /*fdiv*/ ⇒ null
			case 23 /*fload*/ ⇒ null
			case 34 /*fload_0*/ ⇒ null
			case 35 /*fload_1*/ ⇒ null
			case 36 /*fload_2*/ ⇒ null
			case 37 /*fload_3*/ ⇒ null
			case 106 /*fmul*/ ⇒ null
			case 118 /*fneg*/ ⇒ null
			case 114 /*frem*/ ⇒ null
			case 174 /*freturn*/ ⇒ {
				domain.freturn(operands.head)
				new MemoryLayout(List.empty, IndexedSeq.empty)
			}
			case 56 /*fstore*/ ⇒ null
			case 67 /*fstore_0*/ ⇒ null
			case 68 /*fstore_1*/ ⇒ null
			case 69 /*fstore_2*/ ⇒ null
			case 70 /*fstore_3*/ ⇒ null
			case 102 /*fsub*/ ⇒ null
			case 180 /*getfield*/ ⇒ null
			case 178 /*getstatic*/ ⇒ null
			case 167 /*goto*/ ⇒ null
			case 200 /*goto_w*/ ⇒ null
			case 145 /*i2b*/ ⇒ new MemoryLayout(TypedValue.ByteValue :: (operands.tail), locals)
			case 146 /*i2c*/ ⇒ new MemoryLayout(TypedValue.CharValue :: (operands.tail), locals)
			case 135 /*i2d*/ ⇒ new MemoryLayout(TypedValue.DoubleValue :: (operands.tail), locals)
			case 134 /*i2f*/ ⇒ new MemoryLayout(TypedValue.FloatValue :: (operands.tail), locals)
			case 133 /*i2l*/ ⇒ new MemoryLayout(TypedValue.LongValue :: (operands.tail), locals)
			case 147 /*i2s*/ ⇒ new MemoryLayout(TypedValue.ShortValue :: (operands.tail), locals)
			case 96 /*iadd*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 46 /*iaload*/ ⇒ null
			case 126 /*iand*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 79 /*iastore*/ ⇒ null
			case 2 /*iconst_m1*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 3 /*iconst_0*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 4 /*iconst_1*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 5 /*iconst_2*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 6 /*iconst_3*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 7 /*iconst_4*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 8 /*iconst_5*/ ⇒ new MemoryLayout(TypedValue.IntegerValue :: operands, locals)
			case 108 /*idiv*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 165 /*if_acmpeq*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 166 /*if_acmpne*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 159 /*if_icmpeq*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 160 /*if_icmpne*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 161 /*if_icmplt*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 162 /*if_icmpge*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 163 /*if_icmpgt*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 164 /*if_icmple*/ ⇒ new MemoryLayout(operands.tail.tail, locals)
			case 153 /*ifeq*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 154 /*ifne*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 155 /*iflt*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 156 /*ifge*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 157 /*ifgt*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 158 /*ifle*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 199 /*ifnonnull*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 198 /*ifnull*/ ⇒ new MemoryLayout(operands.tail, locals)
			case 132 /*iinc*/ ⇒ this
			case 21 /*iload*/ ⇒ new MemoryLayout(locals(instruction.asInstanceOf[ILOAD].lvIndex) :: operands, locals)
			case 26 /*iload_0*/ ⇒ null
			case 27 /*iload_1*/ ⇒ null
			case 28 /*iload_2*/ ⇒ null
			case 29 /*iload_3*/ ⇒ null
			case 104 /*imul*/ ⇒ null
			case 116 /*ineg*/ ⇒ null
			case 193 /*instanceof*/ ⇒ null
			case 186 /*invokedynamic*/ ⇒ null
			case 185 /*invokeinterface*/ ⇒ null
			case 183 /*invokespecial*/ ⇒ null
			case 184 /*invokestatic*/ ⇒ null
			case 182 /*invokevirtual*/ ⇒ null
			case 128 /*ior*/ ⇒ null
			case 112 /*irem*/ ⇒ null
			case 172 /*ireturn*/ ⇒ {
				domain.ireturn(operands.head)
				new MemoryLayout(List.empty, IndexedSeq.empty)
			}
			case 120 /*ishl*/ ⇒ null
			case 122 /*ishr*/ ⇒ null
			case 54 /*istore*/ ⇒ null
			case 59 /*istore_0*/ ⇒ null
			case 60 /*istore_1*/ ⇒ null
			case 61 /*istore_2*/ ⇒ null
			case 62 /*istore_3*/ ⇒ null
			case 100 /*isub*/ ⇒ null
			case 124 /*iushr*/ ⇒ null
			case 130 /*ixor*/ ⇒ null
			case 168 /*jsr*/ ⇒ null
			case 201 /*jsr_w*/ ⇒ null
			case 138 /*l2d*/ ⇒ null
			case 137 /*l2f*/ ⇒ null
			case 136 /*l2i*/ ⇒ null
			case 97 /*ladd*/ ⇒ null
			case 47 /*laload*/ ⇒ null
			case 127 /*land*/ ⇒ null
			case 80 /*lastore*/ ⇒ null
			case 148 /*lcmp*/ ⇒ null
			case 9 /*lconst_0*/ ⇒ null
			case 10 /*lconst_1*/ ⇒ null
			case 18 /*ldc*/ ⇒ null
			case 19 /*ldc_w*/ ⇒ null
			case 20 /*ldc2_w*/ ⇒ null
			case 109 /*ldiv*/ ⇒ null
			case 22 /*lload*/ ⇒ null
			case 30 /*lload_0*/ ⇒ null
			case 31 /*lload_1*/ ⇒ null
			case 32 /*lload_2*/ ⇒ null
			case 33 /*lload_3*/ ⇒ null
			case 105 /*lmul*/ ⇒ null
			case 117 /*lneg*/ ⇒ null
			case 171 /*lookupswitch*/ ⇒ null
			case 129 /*lor*/ ⇒ null
			case 113 /*lrem*/ ⇒ null
			case 173 /*lreturn*/ ⇒ {
				domain.lreturn(operands.head)
				new MemoryLayout(List.empty, IndexedSeq.empty)
			}
			case 121 /*lshl*/ ⇒ null
			case 123 /*lshr*/ ⇒ null
			case 55 /*lstore*/ ⇒ null
			case 63 /*lstore_0*/ ⇒ null
			case 64 /*lstore_1*/ ⇒ null
			case 65 /*lstore_2*/ ⇒ null
			case 66 /*lstore_3*/ ⇒ null
			case 101 /*lsub*/ ⇒ null
			case 125 /*lushr*/ ⇒ null
			case 131 /*lxor*/ ⇒ null
			case 194 /*monitorenter*/ ⇒ null
			case 195 /*monitorexit*/ ⇒ null
			case 197 /*multianewarray*/ ⇒ null
			case 187 /*new*/ ⇒ null
			case 188 /*newarray*/ ⇒ null
			case 0 /*nop*/ ⇒ null
			case 87 /*pop*/ ⇒ null
			case 88 /*pop2*/ ⇒ null
			case 181 /*putfield*/ ⇒ null
			case 179 /*putstatic*/ ⇒ null
			case 169 /*ret*/ ⇒ null
			case 177 /*return*/ ⇒ null
			case 53 /*saload*/ ⇒ null
			case 86 /*sastore*/ ⇒ null
			case 17 /*sipush*/ ⇒ null
			case 95 /*swap*/ ⇒ null
			case 170 /*tableswitch*/ ⇒ null
			case 196 /*wide*/ ⇒ this // the instructions which are modified by a wide instruction already take care of the effect of wide
		}
	}
}