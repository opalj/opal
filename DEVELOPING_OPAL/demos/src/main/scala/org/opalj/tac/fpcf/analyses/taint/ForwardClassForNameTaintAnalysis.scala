/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.taint

import java.io.File

import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.ForwardTaintAnalysis
import org.opalj.tac.fpcf.analyses.ifds.taint.Taint
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintAnalysis
import org.opalj.tac.fpcf.analyses.ifds.AbsractIFDSAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * A forward IFDS taint analysis, which tracks the String parameters of all methods of the rt.jar,
 * which are callable from outside the library, to calls of Class.forName.
 *
 * @author Dominik Helm
 * @author Mario Trageser
 * @author Michael Eichberg
 */
class ForwardClassForNameTaintAnalysis private (implicit project: SomeProject)
    extends ForwardTaintAnalysis with TaintAnalysis {

    /**
     * The string parameters of all public methods are entry points.
     */
    override def entryPoints: Seq[(DeclaredMethod, Fact)] = for {
        m ← methodsCallableFromOutside.toSeq
        if !m.definedMethod.isNative
        index ← m.descriptor.parameterTypes.zipWithIndex.collect {
            case (pType, index) if pType == ObjectType.String ⇒ index
        }
    } yield (m, Variable(-2 - index))

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean = false

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizeParamters(call: Statement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * This analysis does not create new taints on the fly.
     * Instead, the string parameters of all public methods are tainted in the entry points.
     */
    override protected def createTaints(callee: DeclaredMethod, call: Statement): Set[Fact] =
        Set.empty

    /**
     * Create a FlowFact, if Class.forName is called with a tainted variable for the first parameter.
     */
    override protected def createFlowFact(callee: DeclaredMethod, call: Statement,
                                          in: Set[Fact]): Option[FlowFact] =
        if (isClassForName(callee) && in.contains(Variable(-2)))
            Some(FlowFact(Seq(call.method)))
        else None

    /**
     * We only analyze methods with String parameters (and therefore also in Object parameters).
     * Additionally, we have to analyze Class.forName, so that FlowFacts will be created.
     */
    override protected def relevantCallee(callee: DeclaredMethod): Boolean =
        callee.descriptor.parameterTypes.exists {
            case ObjectType.Object ⇒ true
            case ObjectType.String ⇒ true
            case _                 ⇒ false
        } && (!canBeCalledFromOutside(callee) || isClassForName(callee))

    /**
     * Checks, if a `method` is Class.forName.
     *
     * @param method The method.
     * @return True, if the method is Class.forName.
     */
    private def isClassForName(method: DeclaredMethod): Boolean =
        method.declaringClassType == ObjectType.Class && method.name == "forName"
}

object ForwardClassForNameTaintAnalysis extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore): ForwardClassForNameTaintAnalysis = {
        p.get(RTACallGraphKey)
        new ForwardClassForNameTaintAnalysis()(p)
    }

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}

class ForwardClassForNameAnalysisRunner extends AbsractIFDSAnalysisRunner {

    override def analysisClass: ForwardClassForNameTaintAnalysis.type = ForwardClassForNameTaintAnalysis

    override def printAnalysisResults(analysis: AbstractIFDSAnalysis[_], ps: PropertyStore): Unit =
        for {
            e ← analysis.entryPoints
            flows = ps(e, ForwardClassForNameTaintAnalysis.property.key)
            fact ← flows.ub.asInstanceOf[IFDSProperty[Fact]].flows.values.flatten.toSet[Fact]
        } {
            fact match {
                case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.toJava).mkString(", "))
                case _              ⇒
            }
        }
}

object ForwardClassForNameAnalysisRunner {
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
            new ForwardClassForNameAnalysisRunner().run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-delay"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}
