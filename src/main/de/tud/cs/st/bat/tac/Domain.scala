package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

trait Domain {
    //generic method for dadd
    def arithmeticExpression(cType: ComputationalType, operator: Operator.Value, value2: Value, value1: Value): Value

    def aaload(index: Value, arrayref: Value): Value
    def aastore(value: Value, index: Value, arrayref: Value): Unit
    def nullValue: Value
    def intValue(value: Int): Value
    def longValue(vlaue: Long): Value
    def floatValue(value: Float): Value
    def doubleValue(value: Double): Value
    def anewarray(count: Value, componentType: ReferenceType): Value
    def areturn(value: Value): Unit
    def arraylength(value: Value): Value
    def athrow(value: Value): Value
    def baload(index: Value, arrayref: Value): Value
    def bastore(value: Value, index: Value, arrayref: Value): Unit
    def caload(index: Value, arrayref: Value): Value
    def castore(value: Value, index: Value, arrayref: Value): Unit
    def checkcast(objectref: Value, resolvedType: ReferenceType): Value
    def d2f(value: Value): Value
    def d2i(value: Value): Value
    def d2l(value: Value): Value
    def daload(index: Value, arrayref: Value): Value
    def dastore(value: Value, index: Value, arrayref: Value): Unit
    def f2d(value: Value): Value
    def f2i(value: Value): Value
    def f2l(value: Value): Value
    def fadd(value1: Value, value2: Value): Value
    def fcmpg(value1: Value, value2: Value): Value
    def fcmpl(value1: Value, value2: Value): Value
    def fdiv(value1: Value, value2: Value): Value

    def faload(index: Value, arrayref: Value): Value
    def fastore(value: Value, index: Value, arrayref: Value): Unit

    def ireturn(value: Value): Unit
    def lreturn(value: Value): Unit
    def freturn(value: Value): Unit
    def dreturn(value: Value): Unit
    def returnAddress(value: Int): Value
    def vreturn(): Unit
}