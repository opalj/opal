
package org.opalj.fpcf.analyses

import org.opalj.br.analyses.{BasicReport, Project, ProjectAnalysisApplication}
//import org.opalj.tac.LazyDetachedTACAIKey
//import org.opalj.tac.cg.CFA_1_0_CallGraphKey
//import org.opalj.tac.fpcf.analyses.sql.SqlStringTaintAnalyzer

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

      val insertStatement = "     INSERT              INTO students (name,course, studID )VALUES ('Freddy' , 'Informatik', 'tainted')  ;"

      val x = a.normalizeSQLString(insertStatement)
      analyzeInsert(x)

    }

    ""
  }

  def analyzeInsert(insertStatement: String): Boolean = {
    val pattern = raw"(INSERT).*(INTO)\s+(\w+)\s+(([\w\s,]+))\s+(VALUES)\s+([(\w\s,)]+);".r
    val pattern(command, _, table, columns, _, values) = a.normalizeSQLString(insertStatement)

    println(command)
    println(table)
    println(columns)
    println(values)

    true
  }


  object a {
    val WORD = raw"(\w+|'\w+'|`\w+`)"
    val SPECIAL_WORD = raw"($WORD|(\w+(-|\.|\w+)+\w+)|('\w+(-|\.|\w+)+\w+')|(`\w+(-|\.|\w+)+\w+`))"
    val WORD_LIST = raw"$SPECIAL_WORD(\s*,\s*$SPECIAL_WORD)*"
    val INSERT_COLUMN = raw"\(\s*$WORD_LIST\s*\)"
    val VALUES = raw"\(\s*$WORD_LIST\s*\)(\s*,\s*\(\s*$WORD_LIST\s*\))*"
    val INSERT_PATTERN = raw"((INSERT|insert)\s+((IGNORE|ignore)\s+)?(INTO|into)\s+)($SPECIAL_WORD)\s+$INSERT_COLUMN\s+(VALUES|values)\s+($VALUES)\s*;\s*".r
    val SELECT_PATTERN = raw"(SELECT|select)\s+($WORD_LIST|\*)\s+(FROM|from)\s+($SPECIAL_WORD)\s*(\s+(WHERE|where)\s+.+)?;".r
    val UPDATE_PATTERN = raw"(UPDATE|update)\s+($SPECIAL_WORD)\s+(SET|set)\s+($SPECIAL_WORD\s+=\s+$SPECIAL_WORD)(\s*,\s*$SPECIAL_WORD\s+=\s+$SPECIAL_WORD)*(\s+(WHERE|where) .+)?\s*;\s*".r


    def checkSQLStringSyntax(string: String) = {
      string match {
        case INSERT_PATTERN(_*) => true
        case SELECT_PATTERN(_*) => true
        case UPDATE_PATTERN(_*) => true
        case _ => false
      }
    }

    def normalizeSQLString(sqlString: String): String = {

      val pattern1 = raw"(\s+)"
      val pattern2 = raw"([,;=()])"
      val replacement1 = " "
      val replacement2 = " $1 "

      val sb = new StringBuilder(sqlString)
      sb.replace(0, sb.length(), sb.toString().replaceAll(pattern1, replacement1))
      sb.replace(0, sb.length(), sb.toString().replaceAll(pattern2, replacement2))
      sb.toString().toUpperCase().trim()
    }
  }

}
