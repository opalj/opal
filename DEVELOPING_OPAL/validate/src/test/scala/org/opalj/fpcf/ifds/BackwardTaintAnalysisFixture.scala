/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds
package taint
package old
/*  TODO Fix as soon as backwards analysis is implemented
import org.opalj.br.analyses.SomeProject
import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ifds._
import org.opalj.tac.fpcf.analyses.ifds.taint.{TaintFact, FlowFact, Variable}
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem

case class UnbalancedTaintFact(index: Int, innerFact: TaintFact, callChain: Array[Method])
    extends UnbalancedReturnFact[TaintFact] with TaintFact

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixture(implicit val project: SomeProject)
    extends BackwardIFDSAnalysis(new BackwardTaintProblemFixture(project), OldTaint)

class BackwardTaintProblemFixture(p: SomeProject) extends BackwardTaintProblem(p) {

    override val entryPoints: Seq[(DeclaredMethod, TaintFact)] = p.allProjectClassFiles.filter(classFile =>
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(_.methods).filter(_.name == "sink")
        .map(method => declaredMethods(method) ->
            Variable(JavaIFDSProblem.switchParamAndVariableIndex(0, isStaticMethod = true)))

    /**
     * The sanitize method is the sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizesParameter(call: DeclaredMethodJavaStatement, in: TaintFact): Boolean = false

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(call: DeclaredMethodJavaStatement, in: Set[TaintFact],
                                                source: (DeclaredMethod, TaintFact)): Option[FlowFact] = {
        if (in.exists {
            case Variable(index) => index == call.index
            case _               => false
        } && icfg.getCalleesIfCallStatement(call).get.exists(_.name == "source")) {
            val callChain = currentCallChain(source)
            // Avoid infinite loops.
            if (!containsHeadTwice(callChain))
                Some(FlowFact(callChain.map(JavaMethod(_))))
            else None
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        source:     (DeclaredMethod, TaintFact)
    ): Option[FlowFact] =
        Some(FlowFact(currentCallChain(source).map(JavaMethod(_))))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override protected def createFlowFactAtBeginningOfMethod(
        in:     Set[TaintFact],
        source: (DeclaredMethod, TaintFact)
    ): Option[FlowFact] =
        None
}

object BackwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, TaintFact] = OldTaint
}
*/ 