package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

class TypeDomain extends Domain {
	
	def arithmeticExpression(typed : Type, operator : Operator.Value, value2 : Value, value1 : Value) ={
		TypedValue(typed)
	}
	
	def aaload(index : Value, arrayref : Value) : Value = {
		arrayref match {
			case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
			/* TODO Do we want to handle: case NullValue => …*/
			case _ ⇒ ComputationalTypeValue(ComputationalTypeReference)
		}
	}
	def aastore(value : Value, index : Value, arrayref : Value) { /* Nothing to do. */ }
	def aconstNull() = NullValue
	def anewarray(count : Value, componentType : ReferenceType) : Value = {
		TypedValue(ArrayType(componentType))
	}
	def arraylength(value : Value) : Value = TypedValue.IntegerValue
	def areturn(value : Value) { /* Nothing to do. */ }

	def baload(index : Value, arrayref : Value) : Value = {
		arrayref match {
			case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
			/* TODO Do we want to handle: case NullValue => …*/
			case _ ⇒ ComputationalTypeValue(ComputationalTypeReference)
		}
	}
	def bastore(value : Value, index : Value, arrayref : Value) { /* Nothing to do. */ }
	def caload(index : Value, arrayref : Value) : Value = {
		TypedValue.CharValue
	}
	def castore(value : Value, index : Value, arrayref : Value) { /* Nothing to do. */ }
	
	def checkcast(value : Value,componentType: ReferenceType) = {
		TypedValue(componentType)
	}
	def iconst(constValue : Int) : Value = { TypedValue.IntegerValue }
	def ireturn(value : Value) { /* Nothing to do. */ }

	def freturn(value : Value) { /* Nothing to do. */ }

	def lreturn(value : Value) { /* Nothing to do. */ }

	def dreturn(value : Value) { /* Nothing to do. */ }

	def vreturn() { /* Nothing to do. */ }
}