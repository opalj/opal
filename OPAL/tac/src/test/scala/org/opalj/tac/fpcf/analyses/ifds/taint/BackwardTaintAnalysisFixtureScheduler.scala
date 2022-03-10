/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSAnalysis, BackwardIFDSAnalysis, IFDSAnalysisScheduler, JavaStatement, UnbalancedReturnFact}
import org.opalj.tac.fpcf.properties.{IFDSPropertyMetaInformation, Taint}

case class UnbalancedTaintFact(index: Int, innerFact: Fact, callChain: Array[Method])
    extends UnbalancedReturnFact[Fact] with Fact

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixture(implicit val project: SomeProject)
    extends BackwardIFDSAnalysis(new BackwardTaintProblemFixture(project), Taint)

class BackwardTaintProblemFixture(p: SomeProject) extends BackwardTaintProblem(p) {

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
    override protected def sanitizeParameters(call: JavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(call: JavaStatement, in: Set[Fact],
                                                source: (DeclaredMethod, Fact)): Option[FlowFact[Method]] = {
        if (in.exists {
            case Variable(index) ⇒ index == call.index
            case _               ⇒ false
        } && getCallees(call, source._1).exists(_.name == "source")) {
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
        calleeFact: FlowFact[Method],
        source:     (DeclaredMethod, Fact)
    ): Option[FlowFact[Method]] =
        Some(FlowFact(currentCallChain(source)))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override protected def createFlowFactAtBeginningOfMethod(
        in:     Set[Fact],
        source: (DeclaredMethod, Fact)
    ): Option[FlowFact[Method]] =
        None
}

object BackwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, Fact] = Taint
}
