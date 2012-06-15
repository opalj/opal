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
package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

/**
 * A value on the stack or a value stored in one of the local variables.
 *
 * @note The minimal information that is
 * required by this framework is the computational type of the values. This specific type information is
 * required, e.g., to determine the effect of `dup_...` instructions or the other generic instructions.
 *
 * @author Michael Eichberg
 */
sealed trait Value {
	/**
	 * The computational type of the value.
	 */
	def computationalType : ComputationalType
}

/**
 * Represents a value on the stack or in a local variable for which we have more precise type information
 * than just the computational type value.
 *
 * @author Michael Eichberg
 */
case class TypedValue(valueType : Type) extends Value {
	def computationalType = valueType.computationalType
}
object TypedValue {
	val BooleanValue = TypedValue(BooleanType)
	val ByteValue = TypedValue(ByteType)
	val CharValue = TypedValue(CharType)
	val ShortValue = TypedValue(ShortType)
	val IntegerValue = TypedValue(IntegerType)
	val LongValue = TypedValue(LongType)
	val FloatValue = TypedValue(FloatType)
	val DoubleValue = TypedValue(DoubleType)
}

case class ComputationalTypeValue(val computationalType : ComputationalType) extends Value
object ComputationalTypeValue {
	val CTIntegerValue = ComputationalTypeValue(ComputationalTypeInt)
	val CTFloatValue = ComputationalTypeValue(ComputationalTypeFloat)
	val CTReferenceValue = ComputationalTypeValue(ComputationalTypeReference)
	val CTReturnAddressValue = ComputationalTypeValue(ComputationalTypeReturnAddress)
	val CTLongValue = ComputationalTypeValue(ComputationalTypeLong)
	val CTDoubleValue = ComputationalTypeValue(ComputationalTypeDouble)
}

case object NullValue extends Value {
	def computationalType = ComputationalTypeReference
}

object Operator extends Enumeration {
	type Operator = Value
	val Add, Sub, Mult, Div, Rem = Value
}

case class QCode {

}
object QCode {
	/**
	 * @param classFile Some class file.
	 * @param method A method with a body of the respective given class file.
	 */
	def apply(classFile : ClassFile, method : Method) : QCode = {
		val code = method.body.get
		val initialLocals = {
			var locals : Vector[Value] = Vector.empty
			var localVariableIndex = 0

			if (!method.isStatic) {
				val thisType = classFile.thisClass
				locals = locals.updated(localVariableIndex, TypedValue(thisType))
				localVariableIndex += thisType.computationalType.operandSize
			}
			for (parameterType ← method.descriptor.parameterTypes) {
				val ct = parameterType.computationalType
				locals = locals.updated(localVariableIndex, TypedValue(parameterType))
				localVariableIndex += ct.operandSize
			}
			locals
		}

		// true if the instruction with the respective program counter is already transformed
		val transformed = new Array[Boolean](code.instructions.length)

		var worklist : List[(Int /*program counter*/ , MemoryLayout /* the layout of the locals and stack before the instruction with the respective pc is executed */ )] = List((0, new MemoryLayout(Nil, initialLocals)(new TypeDomain)))
		// the instructions which are at the beginning of a catch block are also added to the catch block
		for (exceptionHandler ← code.exceptionHandlers) {

		}

		while (worklist.nonEmpty) {
			val (pc, memoryLayout) = worklist.head
			worklist = worklist.tail
			if (!transformed(pc)) {

				memoryLayout.update(code.instructions(pc))

				// prepare for the transformation of the next instruction
				transformed(pc) = true
			}
		}

		null
	}
}
/*
trait LValue extends RValue
trait RValue

case class Parameter(val id : Int) extends RValue

case class This extends RValue

trait Statement
trait Expression extends RValue
trait UnaryExpression extends Expression {
	def exp : LValue
}
abstract class BinaryExpression extends Expression {
	def lExp : LValue
	def rExp : LValue
}
case class AndExpression(val lExp : LValue, val rExp : LValue) extends BinaryExpression

case object MonitorEnter extends Statement
case object MonitorExit extends Statement
case class Assignment(lValue : LValue, rValue : RValue) extends Statement

object Demo extends scala.App {
	//    new SEStatement(MonitorEnter, new RValue[ReferenceType] {}) {}
}
*/
