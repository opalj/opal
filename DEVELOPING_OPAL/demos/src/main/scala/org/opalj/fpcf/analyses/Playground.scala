
package org.opalj.fpcf.analyses

import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication}
//import org.opalj.tac.LazyDetachedTACAIKey
//import org.opalj.tac.cg.CFA_1_0_CallGraphKey
import org.opalj.tac.fpcf.analyses.sql.SqlStringTaintAnalyzer

import java.net.URL

object Playground extends ProjectAnalysisApplication{
  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport ={
    val result = analyze(project)
    BasicReport(result)
  }

  def analyze(project: Project[URL]): String = {


    //val computedCallGraph = project.get(CFA_1_0_CallGraphKey)
    //val callGraph = computedCallGraph.numEdges
    //val tacProvider = project.get(LazyDetachedTACAIKey)

    for{
      cf <- project.allProjectClassFiles
      m <- cf.methods
      if m.body.isDefined
      if m.name == "main"
    } {
      //val tac = tacProvider(m)
      //println(tac.cfg)

      val x = "TAINTED_VALUE"
      val insert  = "INSERT INTO tableA ( Id, name ) VALUES ( 01," + x + " );"
      println("bei: ")
      println(insert)
      println(SqlStringTaintAnalyzer.doAnalyze(insert))

      val select = "SELECT * FROM tableA ;";

      val ergebnis = SqlStringTaintAnalyzer.doAnalyze(select)

      println("ergebnis:")
      println(ergebnis)

    //  println(callGraph)
    }

    ""
  }

}
