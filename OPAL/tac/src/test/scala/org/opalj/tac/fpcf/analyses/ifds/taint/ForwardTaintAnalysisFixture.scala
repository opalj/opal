/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{Callable, IFDSAnalysis, IFDSAnalysisScheduler, IFDSFact, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.{JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.Taint

/**
 * An analysis that checks, if the return value of the method `source` can flow to the parameter of
 * the method `sink`.
 *
 * @author Mario Trageser
 */
class ForwardTaintAnalysisFixture(project: SomeProject)
    extends IFDSAnalysis()(project, new ForwardTaintProblemFixture(project), Taint)

class ForwardTaintProblemFixture(p: SomeProject) extends JavaForwardTaintProblem(p) {
    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] =
        p.allProjectClassFiles.filter(classFile =>
            classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
            .flatMap(classFile => classFile.methods)
            .filter(method => method.isPublic && outsideAnalysisContextCall(method).isEmpty)
            .map(method => (method, new IFDSFact(TaintNullFact)))

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: Method, call: JavaStatement): Set[TaintFact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(callee: Method, call: JavaStatement,
                                          in: TaintFact): Option[FlowFact] =
        if (callee.name == "sink" && in == Variable(-2)) Some(FlowFact(Seq(JavaMethod(call.method))))
        else None

    override def createFlowFactAtExit(callee: Method, in: TaintFact, unbCallChain: Seq[Callable]): Option[TaintFact] = None
}

object ForwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new ForwardTaintAnalysisFixture(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))
    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey, DeclaredMethodsKey, PropertyStoreKey)
}
