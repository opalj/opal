/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.old.DeclaredMethodJavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact

case class OldTaint(flows: Map[DeclaredMethodJavaStatement, Set[Fact]], debugData: Map[DeclaredMethodJavaStatement, Set[Fact]] = Map.empty) extends IFDSProperty[DeclaredMethodJavaStatement, Fact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result)
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]], debugData: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result, debugData)

    override def key: PropertyKey[OldTaint] = OldTaint.key
}

object OldTaint extends IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, Fact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result)
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]], debugData: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result, debugData)

    val key: PropertyKey[OldTaint] = PropertyKey.create("OldTaint", new OldTaint(Map.empty))
}
