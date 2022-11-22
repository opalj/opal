
package org.opalj.tac.fpcf.analyses.sql

class TaintMemorySQL(dummyTaintRecognitionWord:String) {

  var tainted: Set[String] = Set(dummyTaintRecognitionWord)
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

    if (taintedTableAndColumns.contains(tableName)) {
      taintedTableAndColumns.get(tableName) match {
        case Some(x) => ((x & columns).size > 0, (x & columns))
        case None => (false, empty)
      }
    } else (false, empty)
  }

}
