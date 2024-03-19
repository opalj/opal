/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties

import org.opalj.fpcf.PropertyKey
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact

/**
 * Represents taint properties
 *
 * @param flows the data flow that leaks a taint
 * @param debugData optional data to debug the analysis
 *
 * @author Marc Clement
 */
case class Taint(
    flows:     Map[JavaStatement, Set[TaintFact]],
    debugData: Map[JavaStatement, Set[TaintFact]] = Map.empty
) extends IFDSProperty[JavaStatement, TaintFact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] =
        Taint(result)
    override def create(
        result:    Map[JavaStatement, Set[TaintFact]],
        debugData: Map[JavaStatement, Set[TaintFact]]
    ): IFDSProperty[JavaStatement, TaintFact] = Taint(result, debugData)

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[JavaStatement, TaintFact] {

    override type Self = Taint
    override def create(result: Map[JavaStatement, Set[TaintFact]]): IFDSProperty[JavaStatement, TaintFact] =
        Taint(result)
    override def create(
        result:    Map[JavaStatement, Set[TaintFact]],
        debugData: Map[JavaStatement, Set[TaintFact]]
    ): IFDSProperty[JavaStatement, TaintFact] = Taint(result, debugData)

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", Taint(Map.empty))
}
