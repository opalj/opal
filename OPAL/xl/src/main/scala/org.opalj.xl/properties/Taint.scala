/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.properties
/*
import org.opalj.fpcf.PropertyKey


case class Taint(flows: Map[Statement, Set[TaintFact]], debugData: Map[LLVMStatement, Set[NativeTaintFact]] = Map.empty) extends IFDSProperty[LLVMStatement, NativeTaintFact] {

    override type Self = Taint
    override def create(result: Map[Statement, Set[TaintFact]]): IFDSProperty[Statement, TaintFact] = new Taint(result)
    override def create(result: Map[Statement, Set[TaintFact]], debugData: Map[Statement, Set[TaintFact]]): IFDSProperty[LLVMStatement, NativeTaintFact] = new NativeTaint(result, debugData)

    override def key: PropertyKey[NativeTaint] = NativeTaint.key
}

object NativeTaint extends IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] {

    override type Self = NativeTaint
    override def create(result: Map[Statement, Set[TaintFact]]): IFDSProperty[Statement, TaintFact] = new NativeTaint(result)
    override def create(result: Map[Statement, Set[TaintFact]], debugData: Map[Statement, Set[TaintFact]]): IFDSProperty[Statement, TaintFact] = new NativeTaint(result, debugData)

    val key: PropertyKey[Taint] = PropertyKey.create("NativeTaint", new Taint(Map.empty))
}
*/