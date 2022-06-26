/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.old.DeclaredMethodJavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact

case class OldTaint(flows: Map[DeclaredMethodJavaStatement, Set[TaintFact]], debugData: Map[DeclaredMethodJavaStatement, Set[TaintFact]] = Map.empty) extends IFDSProperty[DeclaredMethodJavaStatement, TaintFact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[TaintFact]]): IFDSProperty[DeclaredMethodJavaStatement, TaintFact] = new OldTaint(result)
    override def create(result: Map[DeclaredMethodJavaStatement, Set[TaintFact]], debugData: Map[DeclaredMethodJavaStatement, Set[TaintFact]]): IFDSProperty[DeclaredMethodJavaStatement, TaintFact] = new OldTaint(result, debugData)

    override def key: PropertyKey[OldTaint] = OldTaint.key
}

object OldTaint extends IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, TaintFact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[TaintFact]]): IFDSProperty[DeclaredMethodJavaStatement, TaintFact] = new OldTaint(result)
    override def create(result: Map[DeclaredMethodJavaStatement, Set[TaintFact]], debugData: Map[DeclaredMethodJavaStatement, Set[TaintFact]]): IFDSProperty[DeclaredMethodJavaStatement, TaintFact] = new OldTaint(result, debugData)

    val key: PropertyKey[OldTaint] = PropertyKey.create("OldTaint", new OldTaint(Map.empty))
}
