/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opal.js

import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSPropertyMetaInformation}
import org.opalj.js.JavaScriptAwareTaintProblem
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.ifds.taint.{TaintFact, FlowFact, TaintNullFact, Variable}
import org.opalj.tac.fpcf.analyses.ifds.{JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.Taint

/**
 * An analysis that checks, if the return value of the method `source` can flow to the parameter of
 * the method `sink`.
 *
 * @author Mario Trageser
 */
class JavaScriptAwareTaintAnalysisFixture(project: SomeProject)
    extends IFDSAnalysis()(project, new JavaScriptAwareTaintProblemProblemFixture(project), Taint)

class JavaScriptAwareTaintProblemProblemFixture(p: SomeProject) extends JavaScriptAwareTaintProblem(p) {
    /* Without, the tests are unbearably slow. */
    override def useSummaries = true

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(Method, TaintFact)] = p.allProjectClassFiles.filter(classFile =>
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/js/Java2JsTestClass")
        .flatMap(classFile => classFile.methods)
        .filter(method => method.isPublic && outsideAnalysisContext(method).isEmpty)
        .map(method => method -> TaintNullFact)

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
        if (callee.name == "sink" && in == Variable(-2))
            Some(FlowFact(Seq(JavaMethod(call.method))))
        else None
}

object IFDSAnalysisJSFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new JavaScriptAwareTaintAnalysisFixture(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))
    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeProviderKey, DeclaredMethodsKey, PropertyStoreKey)
}
