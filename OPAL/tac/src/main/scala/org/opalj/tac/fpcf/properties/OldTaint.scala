/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.old.DeclaredMethodJavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact

case class OldTaint(flows: Map[DeclaredMethodJavaStatement, Set[Fact]]) extends IFDSProperty[DeclaredMethodJavaStatement, Fact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result)

    override def key: PropertyKey[OldTaint] = OldTaint.key
}

object OldTaint extends IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, Fact] {

    override type Self = OldTaint
    override def create(result: Map[DeclaredMethodJavaStatement, Set[Fact]]): IFDSProperty[DeclaredMethodJavaStatement, Fact] = new OldTaint(result)

    val key: PropertyKey[OldTaint] = PropertyKey.create("OldTaint", new OldTaint(Map.empty))
}
