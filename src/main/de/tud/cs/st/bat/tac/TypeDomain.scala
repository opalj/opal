package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

class TypeDomain extends Domain {

    def nullValue = NullValue
    def intValue(value: Int) = TypedValue.IntegerValue
    def longValue(vlaue: Long) = TypedValue.LongValue
    def floatValue(value: Float) = TypedValue.FloatValue
    def doubleValue(value: Double) = TypedValue.DoubleValue
    def returnAddress(value: Int) = ReturnAddressValue(value)

    //    def arithmeticExpression(typed: Type, operator: Operator.Value, value1: Value, value2: Value) = {
    //        TypedValue(typed)
    //    }

    def aaload(index: Value, arrayref: Value): Value = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            /* TODO Do we want to handle: case NullValue => …*/
            case _                                    ⇒ ComputationalTypeValue(ComputationalTypeReference)
        }
    }
    def aastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def anewarray(count: Value, componentType: ReferenceType): Value = TypedValue(ArrayType(componentType))

    def arraylength(value: Value): Value = TypedValue.IntegerValue
    def areturn(value: Value) { /* Nothing to do. */ }

    def athrow(value: Value): Value = {
        value match {
            case NullValue ⇒ TypedValue(InstructionExceptions.NullPointerException)
            case _         ⇒ value
        }

    }

    def baload(index: Value, arrayref: Value): Value = {
        arrayref match {
            case TypedValue(ArrayType(componentType)) ⇒ TypedValue(componentType)
            /* TODO Do we want to handle: case NullValue => …*/
            case _                                    ⇒ ComputationalTypeValue(ComputationalTypeReference)
        }
    }
    def bastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def caload(index: Value, arrayref: Value): Value = TypedValue.CharValue
    def castore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def daload(index: Value, arrayref: Value): Value = TypedValue.DoubleValue
    def dastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }

    def checkcast(value: Value, resolvedType: ReferenceType) = TypedValue(resolvedType)

    def d2f(value: Value): Value = TypedValue.DoubleValue
    def d2i(value: Value): Value = TypedValue.IntegerValue
    def d2l(value: Value): Value = TypedValue.LongValue
    def f2d(value: Value): Value = TypedValue.DoubleValue
    def f2i(value: Value): Value = TypedValue.IntegerValue
    def f2l(value: Value): Value = TypedValue.LongValue

    def fadd(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def faload(index: Value, arrayref: Value): Value = TypedValue.FloatValue
    def fastore(value: Value, index: Value, arrayref: Value) { /* Nothing to do. */ }
    def fcmpg(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def fcmpl(value1: Value, value2: Value): Value = TypedValue.IntegerValue
    def fdiv(value1: Value, value2: Value): Value = TypedValue.FloatValue
    def freturn(value: Value) { /* Nothing to do. */ }

    def iconst(constValue: Int): Value = { TypedValue.IntegerValue }
    def ireturn(value: Value) { /* Nothing to do. */ }

    def lreturn(value: Value) { /* Nothing to do. */ }

    def dreturn(value: Value) { /* Nothing to do. */ }

    def vreturn() { /* Nothing to do. */ }
}