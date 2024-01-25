/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package taint

import java.io.File

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ifds.IFDSEvaluationRunner
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.AbstractJavaForwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.Taint

/**
 * A forward IFDS taint analysis which tracks the String parameters of all methods of the rt.jar
 * which are callable from outside the library to calls of Class.forName.
 *
 * @author Dominik Helm
 * @author Mario Trageser
 * @author Michael Eichberg
 */
class ForwardClassForNameTaintAnalysis(project: SomeProject)
    extends IFDSAnalysis(project, new ForwardClassForNameTaintProblem(project), Taint)

class ForwardClassForNameTaintProblem(project: SomeProject)
    extends AbstractJavaForwardTaintProblem(project) with TaintProblem[Method, JavaStatement, TaintFact] {

    /**
     * The string parameters of all public methods are entry points.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] = for {
        m <- icfg.methodsCallableFromOutside.toSeq
        if !m.definedMethod.isNative
        index <- m.descriptor.parameterTypes.zipWithIndex.collect {
            case (pType, index) if pType == ObjectType.String => index
        }
    } yield (m.definedMethod, new IFDSFact(Variable(-2 - index)))

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean = false

    /**
     * There is no sanitizing in this analysis.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * This analysis does not create new taints on the fly.
     * Instead, the string parameters of all public methods are tainted in the entry points.
     */
    override protected def createTaints(callee: Method, call: JavaStatement): Set[TaintFact] =
        Set.empty

    /**
     * Create a FlowFact if Class.forName is called with a tainted variable for the first parameter.
     */
    override protected def createFlowFact(callee: Method, call: JavaStatement, in: TaintFact): Option[FlowFact] = {
        if (isClassForName(declaredMethods(callee)) && in == Variable(-2))
            Some(FlowFact(Seq(JavaMethod(call.method))))
        else None
    }

    override def createFlowFactAtExit(callee: Method, in: TaintFact, unbCallChain: Seq[Callable]): Option[TaintFact] =
        None

    /**
     * Checks if a `method` is Class.forName.
     *
     * @param method The method.
     * @return True if the method is Class.forName.
     */
    private def isClassForName(method: DeclaredMethod): Boolean =
        method.declaringClassType == ObjectType.Class && method.name == "forName"
}

object ForwardClassForNameTaintAnalysisScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {

    override def init(p: SomeProject, ps: PropertyStore) = new ForwardClassForNameTaintAnalysis(p)

    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeIteratorKey, PropertyStoreKey, RTACallGraphKey)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI), PropertyBounds.finalP(Callers))
}

class ForwardClassForNameAnalysisRunnerIFDS extends IFDSEvaluationRunner {

    override def analysisClass: ForwardClassForNameTaintAnalysisScheduler.type =
        ForwardClassForNameTaintAnalysisScheduler

    override def printAnalysisResults(analysis: IFDSAnalysis[?, ?, ?], ps: PropertyStore): Unit =
        for {
            e <- analysis.ifdsProblem.entryPoints
            flows = ps(e, ForwardClassForNameTaintAnalysisScheduler.property.key)
            fact <- flows.ub.asInstanceOf[IFDSProperty[JavaStatement, TaintFact]].flows.values.flatten.toSet[TaintFact]
        } {
            fact match {
                case FlowFact(flow) => println(s"flow: " + flow.asInstanceOf[Set[Method]].map(_.toJava).mkString(", "))
                case _              =>
            }
        }
}

object ForwardClassForNameAnalysisRunnerIFDS {
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
            new ForwardClassForNameAnalysisRunnerIFDS().run(
                args.contains("-debug"),
                args.contains("-l2"),
                args.contains("-evalSchedulingStrategies"),
                if (fileIndex >= 0) Some(new File(args(fileIndex + 1))) else None
            )
        }
    }
}
