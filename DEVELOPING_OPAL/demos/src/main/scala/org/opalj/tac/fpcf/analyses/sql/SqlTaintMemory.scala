
package org.opalj.tac.fpcf.analyses.sql

class SqlTaintMemory(defaultTaintIdentifiers:Set[String]) {

  private var taintIdentifiers: Set[String] = defaultTaintIdentifiers
  private val taintedTableAndColumns = scala.collection.mutable.Map[String, Set[String]]()


  /**
   * Adds a new identifier to the set of taint identifiers.
   *
   * @param identifier the identifier to add.
   */
  def addTaintIdentifier(identifier: String): Unit = {
    taintIdentifiers += identifier
  }

  /**
   * checks if the string represent a tainted part or not
   *
   * @param str the String to check.
   * @return true, if the string is a taint identifier
   */
  def isTainted(str: String): Boolean = {
    taintIdentifiers.contains(str)
  }

  /**
   * Adds a set of tainted columns to a table.
   *
   * @param tableName the name of the table.
   * @param columns the set of column names to add.
   */
  def taintTableAndColumns(tableName: String, columns: Set[String]): Unit = {
    val prev = taintedTableAndColumns.getOrElseUpdate(tableName, Set())
    taintedTableAndColumns.update(tableName, prev ++ columns)
  }

  /**
   * Checks if any of the given columns in the specified table are tainted.
   *
   * @param tableName the name of the table to check.
   * @param columns the set of column names to check.
   * @return the set of given columns that have been tainted for the given table.
   */
  def getTaintedColumns(tableName: String, columns: Set[String]): Set[String] = {
    taintedTableAndColumns.get(tableName.trim) match {
      case Some(taintedColumns) => columns.intersect(taintedColumns)
      case None => Set()
    }
  }

  /**
   * retrieves a Set of all columns in the specified table that have been tainted.
   *
   * @param tableName the name of the table to retrieve tainted columns from.
   * @return option value containing a Set of tainted column names associated with the specified tableName. or None if no columns in the table are tainted.
   */
  def getTaintedTableAndColumns(tableName: String): Option[Set[String]] = {
    taintedTableAndColumns.get(tableName)
  }



}
