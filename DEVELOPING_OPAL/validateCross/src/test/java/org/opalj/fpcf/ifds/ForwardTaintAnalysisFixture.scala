/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.AbstractJavaForwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.properties.Taint

import scala.collection.mutable

/**
 * An analysis that checks, if the return value of the method `source` can flow to the parameter of
 * the method `sink`.
 *
 * @author Mario Trageser
 */
class ForwardTaintAnalysisFixture(implicit project: SomeProject)
    extends IFDSAnalysis(project, new ForwardTaintProblemFixture(project), Taint)

class ForwardTaintProblemFixture(p: SomeProject) extends AbstractJavaForwardTaintProblem(p) {
    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] = {
        var temp: mutable.Seq[(Method, IFDSFact[TaintFact, JavaStatement])] = mutable.Seq.empty
        for {
            cf <- p.allProjectClassFiles if cf.thisType == ObjectType("org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
            m <- cf.methods if m.isPublic && outsideAnalysisContextCall(m).isEmpty
        } {
            temp :+= (m -> new IFDSFact(TaintNullFact))
        }
        temp.toSeq
    }

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
    override protected def createFlowFact(
        callee: Method,
        call:   JavaStatement,
        in:     TaintFact
    ): Option[FlowFact] =
        if (callee.name == "sink" && in == Variable(-2)) Some(FlowFact(Seq(JavaMethod(call.method))))
        else None

    override def createFlowFactAtExit(
        callee:              Method,
        in:                  TaintFact,
        unbalancedCallChain: Seq[Callable]
    ): Option[TaintFact] = None
}

object ForwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(p: SomeProject, ps: PropertyStore) = new ForwardTaintAnalysisFixture()(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey, DeclaredMethodsKey, PropertyStoreKey)

    override val uses: Set[PropertyBounds] = PropertyBounds.ubs(Taint)

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] =
        p.get(TypeIteratorKey).usedPropertyKinds
}
