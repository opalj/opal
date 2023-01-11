
package org.opalj.tac.fpcf.analyses.sql

class SqlTaintMemory(dummyTaintRecognitionWords:Set[String]) {

  var tainted: Set[String] = dummyTaintRecognitionWords
  var taintedTableAndColumns = scala.collection.mutable.Map[String, Set[String]]()

  def taint(toTaintedElement: String): Unit = {
    tainted += toTaintedElement
  }

  def isTainted(toInspectElement: String): Boolean = {
    tainted.contains(toInspectElement)
  }

  def clearMemory(): Unit = {
    tainted = Set.empty[String]
  }

  def taintTableAndColums(tableName: String, columns: Set[String]): Unit = {
    val prev = taintedTableAndColumns.get(tableName)

    prev match {
      case Some(x) => taintedTableAndColumns.put(tableName, x ++ columns)
      case None => taintedTableAndColumns.put(tableName, columns)
    }
  }

  def columnsAreTainted(tableName: String, columns: Set[String]) = {
    val empty: Set[String] = Set()

    taintedTableAndColumns.find { case (key, _) => key.trim == tableName.trim } match {
      case Some((_, x)) => ((x & columns).size > 0, (x & columns))
      case None => (false, empty)
    }
  }


}
