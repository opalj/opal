/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.tac.fpcf.analyses.ifds.BackwardIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.UnbalancedReturnFact
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * The unbalanced return fact of this analysis.
 *
 * @param index The index, at which the analyzed method is called by some caller.
 * @param innerFact The fact, which will hold in the caller context after the call.
 * @param callChain The current call chain from the sink.
 */
case class UnbalancedTaintFact(index: Int, innerFact: Fact, callChain: Seq[Method])
    extends UnbalancedReturnFact[Fact] with Fact

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @param project The project, that is analyzed
 * @author Mario Trageser
 */
abstract class BackwardTaintAnalysis(ifdsProblem: BackwardTaintProblem)(implicit val project: SomeProject)
    extends BackwardIFDSAnalysis[Fact, UnbalancedTaintFact](ifdsProblem) {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    override protected def createPropertyValue(result: Map[JavaStatement, Set[Fact]]): IFDSProperty[Fact] =
        new Taint(result)
}