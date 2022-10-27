/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact

case class Taint(flows: Map[JavaStatement, Set[TaintFact]], debugData: Map[JavaStatement, Set[TaintFact]] = Map.empty) extends IFDSProperty[JavaStatement, TaintFact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] = new Taint(result)
    override def create(result: Map[JavaStatement, Set[TaintFact]], debugData: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] = new Taint(result, debugData)

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[JavaStatement, TaintFact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] = new Taint(result)
    override def create(result: Map[JavaStatement, Set[TaintFact]], debugData: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] = new Taint(result, debugData)

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}
