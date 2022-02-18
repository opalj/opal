/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.taint

import java.io.File
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.taint.BackwardTaintAnalysis
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Taint
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.UnbalancedReturnFact
import org.opalj.tac.fpcf.analyses.ifds.taint.ArrayElement
import org.opalj.tac.fpcf.analyses.ifds.taint.InstanceField
import org.opalj.tac.fpcf.analyses.ifds.AbsractIFDSAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * A backward IFDS taint analysis, which tracks the String parameters of all methods of the rt.jar,
 * * which are callable from outside the library, to calls of Class.forName.
 *
 * @author Mario Trageser
 */
class BackwardClassForNameTaintAnalysis private (implicit project: SomeProject)
    extends BackwardTaintAnalysis {

    /**
     * The string parameters of all public methods are entry points.
     */
    override val entryPoints: Seq[(DeclaredMethod, Fact)] =
        p.allProjectClassFiles.filter(classFile ⇒
            classFile.thisType.fqn == "java/lang/Class")
            .flatMap(classFile ⇒ classFile.methods)
            .filter(_.name == "forName")
            .map(method ⇒ declaredMethods(method) → Variable(-2))

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = false

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizeParamters(call: Statement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Do not perform unbalanced return for methods, which can be called from outside the library.
     */
    override protected def shouldPerformUnbalancedReturn(source: (DeclaredMethod, Fact)): Boolean = {
        super.shouldPerformUnbalancedReturn(source) &&
            (!canBeCalledFromOutside(source._1) ||
                // The source is callable from outside, but should create unbalanced return facts.
                entryPoints.contains(source))
    }

    /**
     * This analysis does not create FlowFacts at calls.
     * Instead, FlowFacts are created at the start node of methods.
     */
    override protected def createFlowFactAtCall(call: Statement, in: Set[Fact],
                                                source: (DeclaredMethod, Fact)): Option[FlowFact] = None

    /**
     * This analysis does not create FlowFacts at returns.
     * Instead, FlowFacts are created at the start node of methods.
     */
    protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        source:     (DeclaredMethod, Fact)
    ): Option[FlowFact] = None

    /**
     * If we analyzed a transitive caller of the sink, which is callable from outside the library,
     * and a formal parameter is tainted, we create a FlowFact.
     */
    override protected def createFlowFactAtBeginningOfMethod(
        in:     Set[Fact],
        source: (DeclaredMethod, Fact)
    ): Option[FlowFact] = {
        if (source._2.isInstanceOf[UnbalancedReturnFact[Fact @unchecked]] &&
            canBeCalledFromOutside(source._1) && in.exists {
                // index < 0 means, that it is a parameter.
                case Variable(index) if index < 0            ⇒ true
                case ArrayElement(index, _) if index < 0     ⇒ true
                case InstanceField(index, _, _) if index < 0 ⇒ true
                case _                                       ⇒ false
            }) {
            Some(FlowFact(currentCallChain(source)))
        } else None
    }
}

object BackwardClassForNameTaintAnalysis extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore): BackwardClassForNameTaintAnalysis = {
        p.get(RTACallGraphKey)
        new BackwardClassForNameTaintAnalysis()(p)
    }

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}

class BackwardClassForNameTaintAnalysisRunner extends AbsractIFDSAnalysisRunner {

    override def analysisClass: BackwardClassForNameTaintAnalysis.type = BackwardClassForNameTaintAnalysis

    override def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit = {
        val propertyKey = BackwardClassForNameTaintAnalysis.property.key
        val flowFactsAtSources = ps.entities(propertyKey).collect {
            case EPS((m: DefinedMethod, inputFact)) if canBeCalledFromOutside(m, ps) ⇒
                (m, inputFact)
        }.flatMap(ps(_, propertyKey) match {
            case FinalEP(_, Taint(result)) ⇒
                result.values.fold(Set.empty)((acc, facts) ⇒ acc ++ facts).filter {
                    case FlowFact(_) ⇒ true
                    case _           ⇒ false
                }
            case _ ⇒ Seq.empty
        })
        for {
            fact ← flowFactsAtSources
        } {
            fact match {
                case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.toJava).mkString(", "))
                case _              ⇒
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
}