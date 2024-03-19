/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package taint

import java.io.File

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.IFDSEvaluationRunner
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.ArrayElement
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.InstanceField
import org.opalj.tac.fpcf.analyses.ifds.taint.JavaBackwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.Taint

/**
 * A backward IFDS taint analysis, which tracks the String parameters of all methods of the rt.jar,
 * which are callable from outside the library, to calls of Class.forName.
 *
 * @author Mario Trageser
 */
class BackwardClassForNameTaintAnalysisScheduler private (implicit project: SomeProject)
    extends IFDSAnalysis(project, new BackwardClassForNameTaintProblem(project), Taint)

class BackwardClassForNameTaintProblem(p: SomeProject) extends JavaBackwardTaintProblem(p) {

    /**
     * The string parameters of all public methods are entry points.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] =
        p.allProjectClassFiles.flatMap {
            case cf if cf.thisType == ObjectType.Class =>
                cf.methods.collect {
                    case m if m.name == "forName" => (m, new IFDSFact(Variable(-2)))
                }
        }

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean = false

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * Do not perform unbalanced return for methods tha can be called from outside the library.
     */
    override def shouldPerformUnbalancedReturn(
        source: (Method, IFDSFact[TaintFact, JavaStatement])
    ): Boolean = {
        super.shouldPerformUnbalancedReturn(source) &&
        (!icfg.canBeCalledFromOutside(source._1) ||
        // The source is callable from outside, but should create unbalanced return facts.
        entryPoints.contains(source))
    }

    /**
     * This analysis does not create FlowFacts at calls.
     * Instead, FlowFacts are created at the start node of methods.
     */
    override protected def createFlowFactAtCall(
        call:                JavaStatement,
        in:                  TaintFact,
        unbalancedCallChain: Seq[Callable]
    ): Option[FlowFact] = None

    /**
     * This analysis does not create FlowFacts at returns.
     * Instead, FlowFacts are created at the start node of methods.
     */
    override protected def applyFlowFactFromCallee(
        calleeFact:   FlowFact,
        caller:       Method,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] = None

    /**
     * If we analyzed a transitive caller of the sink, which is callable from outside the library,
     * and a formal parameter is tainted, we create a FlowFact.
     */
    override def createFlowFactAtExit(
        callee:       Method,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] = {
        if (unbCallChain.nonEmpty && // source fact is unbalanced return fact
            icfg.canBeCalledFromOutside(callee) && (in match {
                // index < 0 means that it is a parameter.
                case Variable(index)                         => index < 0
                case ArrayElement(index, _) if index < 0     => true
                case InstanceField(index, _, _) if index < 0 => true
                case _                                       => false
            })
        ) {
            Some(FlowFact(unbCallChain.prepended(JavaMethod(callee))))
        } else None
    }
}

object BackwardClassForNameTaintAnalysisScheduler
    extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(
        p:  SomeProject,
        ps: PropertyStore
    ): BackwardClassForNameTaintAnalysisScheduler = {
        new BackwardClassForNameTaintAnalysisScheduler()(p)
    }

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override def uses: Set[PropertyBounds] =
        Set(PropertyBounds.finalP(TACAI), PropertyBounds.finalP(Callers))

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeIteratorKey, PropertyStoreKey, RTACallGraphKey)
}

class BackwardClassForNameTaintAnalysisRunner extends IFDSEvaluationRunner {

    override def analysisClass: BackwardClassForNameTaintAnalysisScheduler.type =
        BackwardClassForNameTaintAnalysisScheduler

    override def printAnalysisResults(analysis: IFDSAnalysis[?, ?, ?], ps: PropertyStore): Unit = {
        val propertyKey = BackwardClassForNameTaintAnalysisScheduler.property.key
        ps.entities(propertyKey)
            .collect {
                case EPS((m: Method, inputFact))
                    if analysis.ifdsProblem
                        .asInstanceOf[BackwardClassForNameTaintProblem]
                        .icfg
                        .canBeCalledFromOutside(m) =>
                    (m, inputFact)
            }
            .flatMap(ps(_, propertyKey) match {
                case FinalEP(_, Taint(result, _)) =>
                    result.values.fold(Set.empty)((acc, facts) => acc ++ facts).filter {
                        case FlowFact(_) => true
                        case _           => false
                    }
                case _ => Seq.empty
            }).foreach {
                case FlowFact(flow) =>
                    println(s"flow: " + flow.asInstanceOf[Seq[Method]].map(_.toJava).mkString(", "))
                case _ =>
            }

    }
}

object BackwardClassForNameTaintAnalysisRunner {
    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq (to use the SequentialPropertyStore)")
            println(" -l2 (to use the l2 domain instead of the default l1 domain)")
            println(" -debug (for debugging mode in the property store)")
            println(" -evalSchedulingStrategies (evaluates all available scheduling strategies)")
            println(" -f <file> (Stores the average runtime to this file)")
        } else {
            val fileIndex = args.indexOf("-f")
            new BackwardClassForNameTaintAnalysisRunner().run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}
