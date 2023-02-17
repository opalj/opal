/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeTaintFact

case class NativeTaint(flows: Map[LLVMStatement, Set[NativeTaintFact]], debugData: Map[LLVMStatement, Set[NativeTaintFact]] = Map.empty) extends IFDSProperty[LLVMStatement, NativeTaintFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = new NativeTaint(result)
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]], debugData: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = new NativeTaint(result, debugData)

    override def key: PropertyKey[NativeTaint] = NativeTaint.key
}

object NativeTaint extends IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = new NativeTaint(result)
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]], debugData: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = new NativeTaint(result, debugData)

    val key: PropertyKey[NativeTaint] = PropertyKey.create("NativeTaint", new NativeTaint(Map.empty))
}
