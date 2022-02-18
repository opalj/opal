/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.UnbalancedReturnFact
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

case class UnbalancedTaintFact(index: Int, innerFact: Fact, callChain: Array[Method])
    extends UnbalancedReturnFact[Fact] with Fact

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixture private (implicit val pProject: SomeProject)
    extends BackwardTaintAnalysis {

    override val entryPoints: Seq[(DeclaredMethod, Fact)] = p.allProjectClassFiles.filter(classFile ⇒
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(_.methods).filter(_.name == "sink")
        .map(method ⇒ declaredMethods(method) →
            Variable(AbstractIFDSAnalysis.switchParamAndVariableIndex(0, isStaticMethod = true)))

    /**
     * The sanitize method is the sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizeParamters(call: Statement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(call: Statement, in: Set[Fact],
                                                source: (DeclaredMethod, Fact)): Option[FlowFact] = {
        val callPc = call.code(call.index).pc
        if (in.exists {
            case Variable(index) ⇒ index == call.index
            case _               ⇒ false
        } && getCallees(call.node.asBasicBlock, callPc, source._1).exists(_.name == "source")) {
            val callChain = currentCallChain(source)
            // Avoid infinite loops.
            if (!containsHeadTwice(callChain))
                Some(FlowFact(callChain))
            else None
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        source:     (DeclaredMethod, Fact)
    ): Option[FlowFact] =
        Some(FlowFact(currentCallChain(source)))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override protected def createFlowFactAtBeginningOfMethod(
        in:     Set[Fact],
        source: (DeclaredMethod, Fact)
    ): Option[FlowFact] =
        None
}

object BackwardTaintAnalysisFixture extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}
