/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyKey
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ifds.ForwardIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @param project The project, that is analyzed
 * @author Mario Trageser
 */
abstract class ForwardTaintAnalysis(ifdsProblem: ForwardTaintProblem)(implicit val project: SomeProject)
    extends ForwardIFDSAnalysis[Fact](ifdsProblem) {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    val test = classHierarchy

    override protected def createPropertyValue(result: Map[JavaStatement, Set[Fact]]): IFDSProperty[Fact] =
        new Taint(result)
}

/**
 * The IFDSProperty for this analysis.
 */
case class Taint(flows: Map[JavaStatement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {

    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}
