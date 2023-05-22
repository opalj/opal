/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{Callable, IFDSAnalysis, IFDSAnalysisScheduler, IFDSFact, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.JavaBackwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.Taint

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixture(project: SomeProject)
    extends IFDSAnalysis()(project, new BackwardTaintProblemFixture(project), Taint)

class BackwardTaintProblemFixture(p: SomeProject) extends JavaBackwardTaintProblem(p) {

    override def enableUnbalancedReturns: Boolean = true

    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] =
        p.allProjectClassFiles.filter(classFile =>
            classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
            .flatMap(_.methods)
            .filter(_.name == "sink")
            .map(method => method -> new IFDSFact(
                Variable(JavaIFDSProblem.remapParamAndVariableIndex(0, isStaticMethod = true))
            ))

    /**
     * The sanitize method is the sanitizer.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(call: JavaStatement, in: TaintFact,
                                                unbCallChain: Seq[Callable]): Option[FlowFact] = {
        if ((in match {
            case Variable(index) => index == call.index
            case _               => false
        }) && icfg.getCalleesIfCallStatement(call).get.exists(_.name == "source")) {
            val currentMethod = call.callable
            // Avoid infinite loops.
            if (unbCallChain.contains(JavaMethod(currentMethod))) None
            else Some(FlowFact(unbCallChain.prepended(JavaMethod(currentMethod))))
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(calleeFact: FlowFact, caller: Method, in: TaintFact,
                                                   unbCallChain: Seq[Callable]): Option[FlowFact] =
        Some(FlowFact(unbCallChain.prepended(JavaMethod(caller))))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override def createFlowFactAtExit(callee: Method, in: TaintFact,
                                      unbCallChain: Seq[Callable]): Option[FlowFact] = None
}

object BackwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey, DeclaredMethodsKey, PropertyStoreKey)
}