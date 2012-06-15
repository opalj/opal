package de.tud.cs.st.bat
package tac

import resolved.{ Instruction ⇒ BytecodeInstruction }
import resolved._
import resolved.ANEWARRAY

trait Domain {
    def aaload(index: Value, arrayref: Value): Value
    def aastore(value: Value, index: Value, arrayref: Value): Unit
    def aconstNull(): Value
    def anewarray(count: Value, componentType: ReferenceType): Value
    def areturn(value: Value): Unit
    def arraylength(value: Value): Value
    def ireturn(value: Value): Unit
    def lreturn(value: Value): Unit
    def freturn(value: Value): Unit
    def dreturn(value: Value): Unit
    def vreturn(): Unit
}