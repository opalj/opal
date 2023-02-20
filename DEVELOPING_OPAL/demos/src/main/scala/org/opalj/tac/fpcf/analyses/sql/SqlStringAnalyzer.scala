
package org.opalj.tac.fpcf.analyses.sql

object SqlStringAnalyzer {

  /**
   * Object to store information about tainted tables and columns.
   * The specified taint identifiers  are used as input to mark tainted data.
   */
  private var taintMemory = new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))

  /**
   * When 'true', enables console output that provides information on the results of individual helper methods.
   */
  val DEBUG = true

  /**
   *   The specified regex patterns for the types of SQL commands
   */
  val INSERT_PATTERN = "^\\s*(?i)INSERT\\s*(?:IGNORE\\s+)?INTO\\s*(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*(.*);?".r
  val SELECT_PATTERN = "^\\s*(?i)SELECT\\s+(.*)\\s+(?i)FROM\\s+([^\\s]+)(?:\\s+(?i)WHERE\\s+(.+))?\\s*;?".r
  val UPDATE_PATTERN = "^\\s*(?i)UPDATE\\s+([^\\s]+)\\s+(?i)SET\\s+(.+)\\s+(?i)WHERE\\s+(.+)\\s*$".r

  /**
   * Analyzes INSERT, UPDATE or SELECT commands regarding taint information
   *
   * @param sqlString SQL command to be analyzed for taint information
   * @param taintMemorySQL the object used to store and retrieve taint information
   * @return True, if taint information was added or extracted.
   */
  def doAnalyze(sqlString: String, taintMemorySQL: SqlTaintMemory): Boolean = {
    taintMemory = taintMemorySQL
    doAnalyze(sqlString)
  }

  /**
   * Analyzes INSERT, UPDATE or SELECT commands regarding taint information
   *
   * @param sqlString SQL command to be analyzed for taint information
   * @return True, if taint information was added or extracted.
   */
  def doAnalyze(sqlString: String):Boolean = {

    val hasValidSyntax = hasValidSqlSyntax(sqlString)

    //Normalizes the string to a uniform structure to simplify the analyses
    val normalizedString = if (hasValidSyntax) normalizeSQLString(sqlString) else ""

    if(DEBUG){
      println("\n SqlAnalyzer:")
      println(s"given String: $sqlString")
      val message = if (hasValidSyntax) "has valid syntax" else "has invalid syntax"
      println(s"$message")
      println(s"Normalized SQL string: $normalizedString")
    }

    //Decides which type of SQL commands will be analyzed
    val result = normalizedString match {
      case i@INSERT_PATTERN(_*) =>
        analyzeInsertStatement(i)

      case u@UPDATE_PATTERN(_*) =>
        analyzeUpdateStatement(u)

      case s@SELECT_PATTERN(_*) =>
        analyzeSelectStatment(s)

      case _ =>
        if(DEBUG) println("Result of SqlAnalyzer: does not support: "+ sqlString)
        false
    }
    result
  }

  /**
   * Checks the string for specified and valid  SQL syntax
   *
   * @param str  The string to check.
   * @return True, if the string has a recognized valid SQL syntax.
   */
  def hasValidSqlSyntax(str: String): Boolean =
    str match {
      case INSERT_PATTERN(_*) | SELECT_PATTERN(_*) | UPDATE_PATTERN(_*) => true
      case _ => false
    }

  /**
   * Returns a normalized version of the given SQL string by:
   * replacing multiple whitespaces with a single whitespace,
   * surrounding special characters with a whitespace,
   * converting the string to upper case,
   * and removing leading/trailing
   *
   * @param sqlString the SQL string to be normalized
   * @return the normalized SQL string
   */
  def normalizeSQLString(sqlString: String): String = {
    val whitespacePattern = raw"(\s+)"
    val specialCharsPattern = raw"([,;=()])"
    val whitespaceReplacement = " "
    val specialCharsReplacement = " $1 "

      sqlString
      // Remove double whitespace
      .replaceAll(whitespacePattern, whitespaceReplacement)
      //Surround special characters by whitespace
      .replaceAll(specialCharsPattern, specialCharsReplacement)
      // Normalize case sensitivity
      .toUpperCase()
      //remove leading/following spaces
      .trim()
  }

  /**
   * Checks if the values in a column are tainted, based on the INSERT command.
   *
   * @param normalizedString The normalized insert command to be analyzed
   * @return True, if any tainted columns or values were detected.
   */
  def analyzeInsertStatement(normalizedString: String) = {
    val (table, columns, valueGroups) = extractInformationOfInsert(normalizedString)

    //Checks for taint identifiers in table columns.If found, the table is considered to be tainted.
    var taintedColumns: Set[String] = columns.filter(column => taintMemory.isTainted(column)).toSet

    //searches for taint identifiers within value groups, and assigns any columns that contain them as tainted.
    valueGroups.foreach(valueGroup  => {
      for (i <- valueGroup.indices) {
        if (taintMemory.isTainted(valueGroup (i))) taintedColumns += columns(i).trim
      }
    })

    // Checks for taint identifiers in table. If so, all addressed columns are considered as tainted
    if (taintMemory.isTainted(table)) taintedColumns ++= columns.map(str => str.trim)

    //If tainted columns are found, they are recorded along with the corresponding table.
    if (taintedColumns.nonEmpty) taintMemory.taintTableAndColumns(table.trim, taintedColumns)

    if(DEBUG){
      println("analyzeInsertStatement:")
      println("  extracted information:")
      println(s"  table name: $table")
      println(s"  column names: ${columns.mkString("(", ",", ")")}")
      println(s"  valueGroups: ${valueGroups.mkString("(", ",", ")")}")
      println(s"  tainted columns: ${taintedColumns.mkString("(", ",", ")")} of table $table \n")
    }
    taintedColumns.nonEmpty
  }

  /**
   * Extracts addressed fields from an INSERT query
   *
   * @param query the query string to extract information from.
   * @return A tuple containing the extracted table name, column names, and value groups.
   */
  def extractInformationOfInsert(query: String): (String, Seq[String], Seq[Seq[String]]) = {
    val insertPattern = "(?i)INSERT\\s*(?:IGNORE\\s+)?INTO\\s*(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*(.*);?".r

    var extractedTableName = ""
    var extractedColumnNames: Seq[String] = Seq()
    var extractedValueGroups: Seq[Seq[String]] = Seq()
    val pre = query.replace(";","")

    pre match {
      case insertPattern(tableName, columns, values) =>
        extractedTableName = tableName
        extractedColumnNames = columns.split(",").map(_.trim).toSeq
        val removedBrackets = values.split("\\)\\s*,\\s*\\(").map(x => x.replaceAll(raw"(\(|\))", ""))
        extractedValueGroups = removedBrackets.map(x => x.split(",").map(_.trim).toSeq).toSeq

      case _ =>
    }

    (extractedTableName, extractedColumnNames, extractedValueGroups)
  }

  /**
   * Checks if the values in a column are tainted, based on the UPDATE command.
   *
   * @param normalizedString The normalized UPDATE command to be analyzed
   * @return True, if any tainted columns or values were detected.
   */
  def analyzeUpdateStatement(normalizedString: String): Boolean = {
    val (table, columnAndValues) = extractInformationOfUpdate(normalizedString)
    var taintColumns: Set[String] = Set()

    for ((colmn, value) <- columnAndValues) {
      if (taintMemory.isTainted(value) || taintMemory.isTainted(colmn) || taintMemory.isTainted(table)) {
        taintColumns += colmn
      }
    }

    if (taintColumns.nonEmpty) taintMemory.taintTableAndColumns(table, taintColumns)

    if (DEBUG) {
      println("analyzeUpdateStatment:")
      println("  extracted information:")
      println(s"  table name: $table")
      println(s"  column and Values: ${columnAndValues.mkString("(", ",", ")")}")
      println(s"  tainted columns: ${taintColumns.mkString("(", ",", ")")} of table $table \n")
    }
    taintColumns.nonEmpty

  }

  /**
   * Extracts addressed fields from an UPDATE query
   *
   * @param query the query string to extract information from.
   * @return A tuple containing the extracted table name, as well as a sequence of column and value tuples.
   */
  def extractInformationOfUpdate(query: String): (String, Seq[(String, String)]) = {
    val updateRegex = "UPDATE\\s+([^\\s]+)\\s+SET\\s+(.+?)\\s+WHERE\\s+(.+?)\\s*;?\\s*$".r

    var table = ""
    var columnValues: Seq[(String, String)] = Seq()

    updateRegex.findFirstMatchIn(query) match {
      case Some(m) =>
        table = m.group(1)

        //extracts pairs of columns and their corresponding values.
        columnValues = m.group(2).split(",").map(_.trim).toIndexedSeq.map { value =>
          val parts = value.split("=").map(_.trim)
          (parts(0), parts(1))
        }
      case _ =>
    }
    (table, columnValues)
  }


  /**
   * Checks if columns queried in SELECT commands could contain tainted information
   *
   * @param normalizedString The normalized SELECT command to be analyzed
   * @return True, if any of the queried columns is tainted
   */
  def analyzeSelectStatment(normalizedString: String): Boolean = {
    val (tableName, selectedColumns) = extractInformationOfSelect(normalizedString)

    var columnsToTaint: Set[String] = selectedColumns.toSet

    // handles "SELECT * ..."
    if (selectedColumns.contains("*")) {
      taintMemory.getTaintedTableAndColumns(tableName) match {
        case Some(x) => columnsToTaint = x
        case None =>
      }
    }
    if (taintMemory.isTainted(tableName)) taintMemory.taintTableAndColumns(tableName, columnsToTaint)

    val taintedColumns = taintMemory.getTaintedColumns(tableName, columnsToTaint)
    if (DEBUG) {
      println("analyzeSelectStatment:")
      println("  extracted information:")
      println(s"  table name: $tableName")
      println(s"  selected columns: ${selectedColumns.mkString("(", ",", ")")}")
      println(s"  tainted columns: ${taintedColumns.mkString("(", ",", ")")} of table $tableName \n")
    }
    taintedColumns.nonEmpty
  }

  /**
   * Extracts addressed fields from an SELECT query
   *
   * @param query the string to extract information from.
   * @return A tuple containing the extracted table name, as well as a sequence of column
   */
  def extractInformationOfSelect(query: String): (String, Seq[String]) = {
    val selectRegex = "SELECT\\s+(DISTINCT\\s+)?(.*?)\\s+FROM\\s+([^\\s]+)\\s*(WHERE\\s+(.*))?".r
    var table = ""
    var columns: Seq[String] = Seq()
    selectRegex.findFirstMatchIn(query) match {
      case Some(str) =>
        columns = str.group(2).split(",").map(_.trim).toSeq
        table = str.group(3)
      case _ =>
    }
    (table, columns)
  }

  def getTaintMemory(): SqlTaintMemory ={
    taintMemory
  }


}
