/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement

/**
 * IFDS property representing a native taint in the taint analysis.
 *
 * @param flows
 * @param debugData
 *
 * @author Marc Clement
 */
case class NativeTaint(
        flows:     Map[LLVMStatement, Set[NativeTaintFact]],
        debugData: Map[LLVMStatement, Set[NativeTaintFact]] = Map.empty
) extends IFDSProperty[LLVMStatement, NativeTaintFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = NativeTaint(result)
    override def create(
        result:    Map[LLVMStatement, Set[NativeTaintFact]],
        debugData: Map[LLVMStatement, Set[NativeTaintFact]]
    ): IFDSProperty[LLVMStatement, NativeTaintFact] = NativeTaint(result, debugData)

    override def key: PropertyKey[NativeTaint] = NativeTaint.key
}

object NativeTaint extends IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] {

    override type Self = NativeTaint
    override def create(result: Map[LLVMStatement, Set[NativeTaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = NativeTaint(result)
    override def create(
        result:    Map[LLVMStatement, Set[NativeTaintFact]],
        debugData: Map[LLVMStatement, Set[NativeTaintFact]]
    ): IFDSProperty[LLVMStatement, NativeTaintFact] = NativeTaint(result, debugData)

    val key: PropertyKey[NativeTaint] = PropertyKey.create("NativeTaint", NativeTaint(Map.empty))
}
