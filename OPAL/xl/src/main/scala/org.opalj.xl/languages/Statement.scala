/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.languages

import org.opalj.fpcf.Entity

case class GlobalVariable(name: String) extends Entity

class L() {
  class Expression() extends Entity

  case class Variable(name: String) extends Entity

  case class Num(value: Int) extends Expression

  case class Assignment(variable: Any, value: Entity) extends Expression

  case class Function(name:String, params: List[Variable], Body: List[Assignment]) extends Expression

  case class F(language: L, val name: String) extends Expression

  case class FunctionCall(name: String, params: List[Variable]) extends Expression

  case class ForeignFunctionCall(language: L, name: String, params: List[Variable]) extends Expression

}
object L extends L

object L0 extends L
object L1 extends L
object L2 extends L
