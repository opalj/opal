/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.tac.fpcf.analyses.ifds.NewJavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact

case class NewTaint(flows: Map[NewJavaStatement, Set[Fact]]) extends IFDSProperty[NewJavaStatement, Fact] {

    override type Self = NewTaint
    override def create(result: Map[NewJavaStatement, Set[Fact]]): IFDSProperty[NewJavaStatement, Fact] = new NewTaint(result)

    override def key: PropertyKey[NewTaint] = NewTaint.key
}

object NewTaint extends IFDSPropertyMetaInformation[NewJavaStatement, Fact] {

    override type Self = NewTaint
    override def create(result: Map[NewJavaStatement, Set[Fact]]): IFDSProperty[NewJavaStatement, Fact] = new NewTaint(result)

    val key: PropertyKey[NewTaint] = PropertyKey.create("NewTaint", new NewTaint(Map.empty))
}
