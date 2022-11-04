/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSFact, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeIteratorKey
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

    override def followUnbalancedReturns: Boolean = true

    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, Method])] = p.allProjectClassFiles.filter(classFile =>
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(_.methods)
        .filter(_.name == "sink")
        .map(method => method -> new IFDSFact(
            Variable(JavaIFDSProblem.switchParamAndVariableIndex(0, isStaticMethod = true))
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
                                                unbCallChain: Seq[Method]): Option[FlowFact] = {
        if ((in match {
            case Variable(index) => index == call.index
            case _               => false
        }) && icfg.getCalleesIfCallStatement(call).get.exists(_.name == "source")) {
            val currentMethod = call.callable
            // Avoid infinite loops.
            if (unbCallChain.contains(currentMethod)) None
            else Some(FlowFact(unbCallChain.prepended(currentMethod).map(JavaMethod)))
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(calleeFact: FlowFact, callee: Method, in: TaintFact,
                                                   unbCallChain: Seq[Method]): Option[FlowFact] =
        Some(FlowFact(unbCallChain.prepended(callee).map(JavaMethod)))

    // TODO
    ///**
    // * This analysis does not create FlowFacts at the beginning of a method.
    // * Instead, FlowFacts are created, when the return value of source is tainted.
    // */
    //override protected def createFlowFactAtBeginningOfMethod(
    //    in:     Set[TaintFact],
    //    source: (DeclaredMethod, TaintFact)
    //): Option[FlowFact] =
    //    None
}

///**
// * Called, when a new fact is found at the entry (entry of analysis) of a method.
// * Creates a fact if necessary.
// *
// * @param in     The newly found fact.
// * @param callee The callee.
// * @return Some fact, if necessary. Otherwise None.
// */
//def createFlowFactAtEntry(callee: C, in: Fact): Option[Fact] = None
//
///**
// * Called, when the analysis found a new output fact at a function's exit (exit of analysis).
// * A concrete analysis may overwrite this method to create additional facts, which will be added
// * to the analysis' result.
// *
// * @param in     The new output fact at the exit (exit of analysis) node.
// * @param callee The function containing the respective exit statement.
// * @return Nothing by default.
// */
//def createFactsAtExit(in: Fact, callee: C): Set[Fact] = Set.empty

object BackwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey, DeclaredMethodsKey, PropertyStoreKey)
}