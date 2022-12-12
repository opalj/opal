/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package coordinator

import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.xl.analyses.a0.A0Tainted
import org.opalj.xl.analyses.a1.A1Tainted
import org.opalj.xl.analyses.a2.A2Tainted
import org.opalj.xl.analyses.java.analysis.EagerJavaTaintAnalysis

import java.net.URL

    object TaintRunner extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult]{ //},[URL] ReportableAnalysisResult] {

      implicit val logContext: LogContext = GlobalLogContext

      final override val analysis = this

      //override project = setupProject(List(new File("/Users/tobiasroth/Documents/Projects/opal/OPAL/xl/target/scala-2.13/classes/org/opalj/xl/analyses/java/code/Main.class")),
      //  List.empty, false, ConfigFactory.load())

      override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
          EagerJavaTaintAnalysis/*
          EagerA0TaintAnalysis,
          EagerA1TaintAnalysis,
          EagerA2TaintAnalysis,
          LazyFunctionTranslatorAnalysis*/
        )

        val a0TaintedVariables = propertyStore.finalEntities(A0Tainted)
        val a1TaintedVariables = propertyStore.finalEntities(A1Tainted)
        val a2TaintedVariables = propertyStore.finalEntities(A2Tainted)

        BasicReport(
          " \n" +
            s"""
               | Results of A0TaintAnalysis:
               | Tainted variables: ${a0TaintedVariables.mkString("\n")}
               |
               | Results of A1TaintAnalysis:
               | Tainted variables: ${a1TaintedVariables.mkString("\n")}
               |
               | Results of A2TaintAnalysis:
               | Tainted variables: ${a2TaintedVariables.mkString("\n")}
               |
               |""".stripMargin)
      } }




/*
  def start() = {



  A1.analyze(this, l1State, program)
    println(
      s"""
         | L1State: ${l1State.store.mkString(";\n")}
         |
         | L2State: ${l2State.store.mkString(";\n")}
         |
         | L3State: ${l3State.store.mkString(";\n")}
         |
         |""".stripMargin)
  }
*/


/*
  def suspend(preState: State, statement: Entity, code: List[Entity]) = { //}, cont: A => Unit) = {
    val result = InterfaceChecker.check(statement)
    result._1 match {
      case L1 =>
        val newState = StateTransformer.transform(preState, l1State)
        A1.analyze(this, newState, code)
        StateTransformer.transform(l1State, preState)
      case L2 =>
        A2.analyze(this, StateTransformer.transform(preState, l2State), code)
        StateTransformer.transform(l2State, preState)
      case L3 =>
        A3.analyze(this, StateTransformer.transform(preState, l3State), code)
        StateTransformer.transform(l3State, preState)
    }
    println(s"Prestate: ${preState.store.mkString("\n")}")
  }
}


object Runner {
  def main(args: Array[String]): Unit = {
    new Coordinator().start()
  }
}
*/
