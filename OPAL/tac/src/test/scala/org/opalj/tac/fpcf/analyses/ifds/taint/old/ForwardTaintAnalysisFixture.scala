/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint.old

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.old.{DeclaredMethodJavaStatement, ForwardIFDSAnalysis, IFDSAnalysisScheduler}
import org.opalj.tac.fpcf.analyses.ifds.old.taint.ForwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, NullFact, Variable}
import org.opalj.tac.fpcf.properties.OldTaint

/**
 * An analysis that checks, if the return value of the method `source` can flow to the parameter of
 * the method `sink`.
 *
 * @author Mario Trageser
 */
class ForwardTaintAnalysisFixture(implicit val project: SomeProject)
    extends ForwardIFDSAnalysis(new ForwardTaintProblemFixture(project), OldTaint)

class ForwardTaintProblemFixture(p: SomeProject) extends ForwardTaintProblem(p) {

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(DeclaredMethod, Fact)] = p.allProjectClassFiles.filter(classFile ⇒
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(classFile ⇒ classFile.methods)
        .filter(method ⇒ method.isPublic && outsideAnalysisContext(declaredMethods(method)).isEmpty)
        .map(method ⇒ declaredMethods(method) → NullFact)

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizeParameters(call: DeclaredMethodJavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: DeclaredMethod, call: DeclaredMethodJavaStatement): Set[Fact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(callee: DeclaredMethod, call: DeclaredMethodJavaStatement,
                                          in: Set[Fact]): Option[FlowFact] =
        if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(JavaMethod(call.method))))
        else None
}

object ForwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new ForwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, Fact] = OldTaint
}
