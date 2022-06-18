/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact

case class Taint(flows: Map[JavaStatement, Set[Fact]], debugData: Map[JavaStatement, Set[Fact]] = Map.empty) extends IFDSProperty[JavaStatement, Fact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[Fact]]): IFDSProperty[JavaStatement, Fact] = new Taint(result)
    override def create(result: Map[JavaStatement, Set[Fact]], debugData: Map[JavaStatement, Set[Fact]]): IFDSProperty[JavaStatement, Fact] = new Taint(result, debugData)

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[JavaStatement, Fact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[Fact]]): IFDSProperty[JavaStatement, Fact] = new Taint(result)
    override def create(result: Map[JavaStatement, Set[Fact]], debugData: Map[JavaStatement, Set[Fact]]): IFDSProperty[JavaStatement, Fact] = new Taint(result, debugData)

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}
