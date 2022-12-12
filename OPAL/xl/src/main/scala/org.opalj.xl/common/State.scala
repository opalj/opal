/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.common

import org.opalj.fpcf.Entity

import scala.collection.mutable

trait State {
  val store = new mutable.HashMap[Entity, Value]()

  def set(tuple1: Tuple2[Entity, Value]) = store.put(tuple1._1, tuple1._2)

  def get(entity: Entity): Option[Value] = store.get(entity)

  val functions = new mutable.HashMap[String, Entity]()
}

case class L1State() extends State
case class L2State() extends State
case class L3State() extends State