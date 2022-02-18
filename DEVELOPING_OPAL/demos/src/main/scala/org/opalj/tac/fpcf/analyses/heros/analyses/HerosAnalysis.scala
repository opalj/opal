/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses

import java.io.File
import java.io.PrintWriter

import heros.template.DefaultIFDSTabulationProblem
import heros.solver.IFDSSolver
import javax.swing.JOptionPane
import org.opalj.bytecode

import org.opalj.util.Milliseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalICFG
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.Stmt
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V

/**
 * A common subclass of all Heros analyses.
 *
 * @param p The project, which will be analyzed.
 * @param icfg The project's control flow graph.
 * @tparam F The type of data flow facts of the analysis.
 * @author Mario Trageser
 */
abstract class HerosAnalysis[F](p: SomeProject, icfg: OpalICFG)
    extends DefaultIFDSTabulationProblem[Statement, F, Method, OpalICFG](icfg) {

    /**
     * The project's property store.
     */
    protected implicit val propertyStore: PropertyStore = p.get(PropertyStoreKey)

    /**
     * The project's declared methods.
     */
    protected implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    /**
     * The number of threads can be configured with NUM_THREADS.
     */
    override def numThreads(): Int = HerosAnalysis.NUM_THREADS

    /**
     * Gets the Call for a statement that contains a call (MethodCall Stmt or ExprStmt/Assigment
     * with FunctionCall)
     */
    protected def asCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
        case Assignment.ASTID ⇒ stmt.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   ⇒ stmt.asExprStmt.expr.asFunctionCall
        case _                ⇒ stmt.asMethodCall
    }
}

object HerosAnalysis {

    /**
     * The number of threads, with which analyses should run.
     */
    var NUM_THREADS = 1

    /**
     * If this flag is set, a JOptionPane is displayed before and after each analysis run in
     * evalProject. By doing so, it is easier to measure the memory consumption with VisualVM.
     */
    var MEASURE_MEMORY = false

    /**
     * Checks, if some method can be called from outside.
     *
     * @param method The method.
     * @return True, if the method can be called from outside.
     */
    def canBeCalledFromOutside(method: Method)(implicit declaredMethods: DeclaredMethods, propertyStore: PropertyStore): Boolean = {
        val FinalEP(_, callers) = propertyStore(declaredMethods(method), Callers.key)
        callers.hasCallersWithUnknownContext
    }
}

/**
 * An abstract runner for Heros analyses.
 *
 * @tparam F The type of data flow facts of the analysis.
 * @tparam Analysis The analysis, which will be executed.
 * @author Mario Trageser
 */
abstract class HerosAnalysisRunner[F, Analysis <: HerosAnalysis[F]] {

    /**
     * Creates the analysis, which will be executed.
     *
     * @param p The project, which will be analyzed.
     * @return The analysis, which will be executed.
     */
    protected def createAnalysis(p: SomeProject): Analysis

    /**
     * Prints the analysis results to the console.
     *
     * @param analysis The analysis, which was executed.
     * @param analysisTime The time, the analysis needed, in milliseconds.
     */
    protected def printResultsToConsole(analysis: Analysis, analysisTime: Milliseconds): Unit

    /**
     * Executes the analysis NUM_EXECUTIONS times, prints the analysis results to the console
     * and saves evaluation data in evaluationFile, if present.
     *
     * @param evaluationFile Evaluation data will be saved in this file, if present.
     */
    def run(evaluationFile: Option[File]): Unit = {
        val p = Project(bytecode.RTJar)
        var times = Seq.empty[Milliseconds]
        for {
            _ ← 1 to HerosAnalysisRunner.NUM_EXECUTIONS
        } {
            times :+= evalProject(Project.recreate(p))
        }
        if (evaluationFile.isDefined) {
            val pw = new PrintWriter(evaluationFile.get)
            pw.println(s"Average time: ${times.map(_.timeSpan).sum / times.size}ms")
            pw.close()
        }
    }

    /**
     * Executes the analysis.
     *
     * @param p The project, which will be analyzed.
     * @return The time, the analysis needed.
     */
    private def evalProject(p: SomeProject): Milliseconds = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               ⇒ Set(classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]])
            case Some(requirements) ⇒ requirements + classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        }
        p.get(RTACallGraphKey)
        val analysis = createAnalysis(p)
        val solver = new IFDSSolver(analysis)
        var analysisTime: Milliseconds = Milliseconds.None
        if (HerosAnalysis.MEASURE_MEMORY)
            JOptionPane.showMessageDialog(null, "Call Graph finished")
        time {
            solver.solve()
        } { t ⇒ analysisTime = t.toMilliseconds }
        if (HerosAnalysis.MEASURE_MEMORY)
            JOptionPane.showMessageDialog(null, "Analysis finished")
        printResultsToConsole(analysis, analysisTime)
        analysisTime
    }
}

object HerosAnalysisRunner {

    /**
     * The number of analysis runs, which will be performed by the run method.
     */
    var NUM_EXECUTIONS = 10
}