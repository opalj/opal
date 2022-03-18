/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.{IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeFact

case class NativeTaint(flows: Map[LLVMStatement, Set[NativeFact]]) extends IFDSProperty[LLVMStatement, NativeFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeFact]]): IFDSProperty[LLVMStatement, NativeFact] = new NativeTaint(result)

    override def key: PropertyKey[NativeTaint] = NativeTaint.key
}

object NativeTaint extends IFDSPropertyMetaInformation[LLVMStatement, NativeFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeFact]]): IFDSProperty[LLVMStatement, NativeFact] = new NativeTaint(result)

    val key: PropertyKey[NativeTaint] = PropertyKey.create("NativeTaint", new NativeTaint(Map.empty))
}