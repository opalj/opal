/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.taint
/*  TODO Fix as soon as backwards analysis is implemented
import org.opalj.br.analyses.SomeProject
import org.opalj.br.{DeclaredMethod, DefinedMethod, Method}
import org.opalj.fpcf.{EPS, FinalEP, PropertyStore}
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.old.taint.BackwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.old._
import org.opalj.tac.fpcf.analyses.ifds.taint._
import org.opalj.tac.fpcf.properties.OldTaint

import java.io.File

/**
 * A backward IFDS taint analysis, which tracks the String parameters of all methods of the rt.jar,
 * * which are callable from outside the library, to calls of Class.forName.
 *
 * @author Mario Trageser
 */
class BackwardClassForNameTaintAnalysisScheduler private (implicit val project: SomeProject)
    extends BackwardIFDSAnalysis(new BackwardClassForNameTaintProblem(project), OldTaint)

class BackwardClassForNameTaintProblem(p: SomeProject) extends BackwardTaintProblem(p) {

    /**
     * The string parameters of all public methods are entry points.
     */
    override val entryPoints: Seq[(DeclaredMethod, TaintFact)] =
        p.allProjectClassFiles.filter(classFile =>
            classFile.thisType.fqn == "java/lang/Class")
            .flatMap(classFile => classFile.methods)
            .filter(_.name == "forName")
            .map(method => declaredMethods(method) -> Variable(-2))

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = false

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesParameter(call: DeclaredMethodJavaStatement, in: TaintFact): Boolean = false

    /**
     * Do not perform unbalanced return for methods, which can be called from outside the library.
     */
    override def shouldPerformUnbalancedReturn(source: (DeclaredMethod, TaintFact)): Boolean = {
        super.shouldPerformUnbalancedReturn(source) &&
            (!canBeCalledFromOutside(source._1) ||
                // The source is callable from outside, but should create unbalanced return facts.
                entryPoints.contains(source))
    }

    /**
     * This analysis does not create FlowFacts at calls.
     * Instead, FlowFacts are created at the start node of methods.
     */
    override protected def createFlowFactAtCall(call: DeclaredMethodJavaStatement, in: Set[TaintFact],
                                                source: (DeclaredMethod, TaintFact)): Option[FlowFact] = None

    /**
     * This analysis does not create FlowFacts at returns.
     * Instead, FlowFacts are created at the start node of methods.
     */
    protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        source:     (DeclaredMethod, TaintFact)
    ): Option[FlowFact] = None

    /**
     * If we analyzed a transitive caller of the sink, which is callable from outside the library,
     * and a formal parameter is tainted, we create a FlowFact.
     */
    override protected def createFlowFactAtBeginningOfMethod(
        in:     Set[TaintFact],
        source: (DeclaredMethod, TaintFact)
    ): Option[FlowFact] = {
        if (source._2.isInstanceOf[UnbalancedReturnFact[TaintFact @unchecked]] &&
            canBeCalledFromOutside(source._1) && in.exists {
                // index < 0 means, that it is a parameter.
                case Variable(index) if index < 0            => true
                case ArrayElement(index, _) if index < 0     => true
                case InstanceField(index, _, _) if index < 0 => true
                case _                                       => false
            }) {
            Some(FlowFact(currentCallChain(source).map(JavaMethod(_))))
        } else None
    }
}

object BackwardClassForNameTaintAnalysisScheduler extends IFDSAnalysisScheduler[TaintFact] {

    override def init(p: SomeProject, ps: PropertyStore): BackwardClassForNameTaintAnalysisScheduler = {
        p.get(RTACallGraphKey)
        new BackwardClassForNameTaintAnalysisScheduler()(p)
    }

    override def property: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, TaintFact] = OldTaint
}

class BackwardClassForNameTaintAnalysisRunner extends AbsractIFDSAnalysisRunner {

    override def analysisClass: BackwardClassForNameTaintAnalysisScheduler.type = BackwardClassForNameTaintAnalysisScheduler

    override def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit = {
        val propertyKey = BackwardClassForNameTaintAnalysisScheduler.property.key
        val flowFactsAtSources = ps.entities(propertyKey).collect {
            case EPS((m: DefinedMethod, inputFact)) if canBeCalledFromOutside(m, ps) =>
                (m, inputFact)
        }.flatMap(ps(_, propertyKey) match {
            case FinalEP(_, OldTaint(result, _)) =>
                result.values.fold(Set.empty)((acc, facts) => acc ++ facts).filter {
                    case FlowFact(_) => true
                    case _           => false
                }
            case _ => Seq.empty
        })
        for {
            fact <- flowFactsAtSources
        } {
            fact match {
                case FlowFact(flow) => println(s"flow: "+flow.asInstanceOf[Seq[Method]].map(_.toJava).mkString(", "))
                case _              =>
            }
        }
    }
}

object BackwardClassForNameTaintAnalysisRunner {
    def main(args: Array[String]): Unit = {
        if (args.contains("--help")) {
            println("Potential parameters:")
            println(" -seq (to use the SequentialPropertyStore)")
            println(" -l2 (to use the l2 domain instead of the default l1 domain)")
            println(" -delay (for a three seconds delay before the taint flow analysis is started)")
            println(" -debug (for debugging mode in the property store)")
            println(" -evalSchedulingStrategies (evaluates all available scheduling strategies)")
            println(" -f <file> (Stores the average runtime to this file)")
        } else {
            val fileIndex = args.indexOf("-f")
            new BackwardClassForNameTaintAnalysisRunner().run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-delay"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}*/ 