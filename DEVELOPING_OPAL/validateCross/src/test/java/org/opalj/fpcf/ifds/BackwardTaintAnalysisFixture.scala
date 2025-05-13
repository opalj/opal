/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import org.opalj.br.Method
import org.opalj.br.ClassType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.JavaBackwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.properties.Taint

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixture(project: SomeProject)
    extends IFDSAnalysis(project, new BackwardTaintProblemFixture(project), Taint)

class BackwardTaintProblemFixture(p: SomeProject) extends JavaBackwardTaintProblem(p) {

    override def enableUnbalancedReturns: Boolean = true

    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] =
        p.allProjectClassFiles.flatMap {
            case cf if cf.thisType == ClassType("org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass") =>
                cf.methods.collect {
                    case m if m.name == "sink" =>
                        m -> new IFDSFact(
                            Variable(JavaIFDSProblem.remapParamAndVariableIndex(0, isStaticMethod = true))
                        )
                }
            case _ => None
        }

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
    override protected def createFlowFactAtCall(
        call:         JavaStatement,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] = {
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
    override protected def applyFlowFactFromCallee(
        calleeFact:          FlowFact,
        caller:              Method,
        in:                  TaintFact,
        unbalancedCallChain: Seq[Callable]
    ): Option[FlowFact] =
        Some(FlowFact(unbalancedCallChain.prepended(JavaMethod(caller))))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override def createFlowFactAtExit(
        callee:              Method,
        in:                  TaintFact,
        unbalancedCallChain: Seq[Callable]
    ): Option[FlowFact] = None
}

object BackwardTaintAnalysisFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(p: SomeProject, ps: PropertyStore) = new BackwardTaintAnalysisFixture(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey, DeclaredMethodsKey, PropertyStoreKey)

    override val uses: Set[PropertyBounds] = PropertyBounds.ubs(Taint)

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] =
        p.get(TypeIteratorKey).usedPropertyKinds
}