/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.tac.fpcf.analyses.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * An analysis that checks, if the return value of the method `source` can flow to the parameter of
 * the method `sink`.
 *
 * @author Mario Trageser
 */
class ForwardTaintAnalysisFixture private (implicit val pProject: SomeProject)
    extends ForwardTaintAnalysis {

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(DeclaredMethod, Fact)] = p.allProjectClassFiles.filter(classFile ⇒
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(classFile ⇒ classFile.methods)
        .filter(method ⇒ method.isPublic && insideAnalysisContext(declaredMethods(method)))
        .map(method ⇒ declaredMethods(method) → NullFact)

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = callee.name == "sanitize"

    /**
     * We do not sanitize paramters.
     */
    override protected def sanitizeParamters(call: Statement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: DeclaredMethod, call: Statement): Set[Fact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(callee: DeclaredMethod, call: Statement,
                                          in: Set[Fact]): Option[FlowFact] =
        if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(call.method)))
        else None
}

object ForwardTaintAnalysisFixture extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new ForwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}
