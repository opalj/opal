/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.properties.{IFDSProperty, IFDSPropertyMetaInformation}

case class NativeTaint(flows: Map[LLVMStatement, Set[Fact]]) extends IFDSProperty[LLVMStatement, Fact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[Fact]]): IFDSProperty[LLVMStatement, Fact] = new NativeTaint(result)

    override def key: PropertyKey[NativeTaint] = NativeTaint.key
}

object NativeTaint extends IFDSPropertyMetaInformation[LLVMStatement, Fact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[Fact]]): IFDSProperty[LLVMStatement, Fact] = new NativeTaint(result)

    val key: PropertyKey[NativeTaint] = PropertyKey.create("NativeTaint", new NativeTaint(Map.empty))
}