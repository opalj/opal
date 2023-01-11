
package org.opalj.tac.fpcf.analyses.sql

object SqlStringTaintAnalyzer {

  //Regex Pattern:{

  /*
  val WORD = raw"(\w+|'\w+'|`\w+`)"
  val SPECIAL_WORD = raw"($WORD|(\w+(-|\.|\w+)+\w+)|('\w+(-|\.|\w+)+\w+')|(`\w+(-|\.|\w+)+\w+`))"
  val WORD_LIST = raw"$SPECIAL_WORD(\s*,\s*$SPECIAL_WORD)*"
  val INSERT_COLUMN = raw"\(\s*$WORD_LIST\s*\)"
  val VALUES = raw"\(\s*$WORD_LIST\s*\)(\s*,\s*\(\s*$WORD_LIST\s*\))*"
  val INSERT_PATTERN = raw"((INSERT|insert)\s+((IGNORE|ignore)\s+)?(INTO|into)\s+)($SPECIAL_WORD)\s+$INSERT_COLUMN\s+(VALUES|values)\s+($VALUES)\s*;\s*".r
  val SELECT_PATTERN = raw"(SELECT|select)\s+($WORD_LIST|\*)\s+(FROM|from)\s+($SPECIAL_WORD)\s*(\s+(WHERE|where)\s+.+)?;".r
  val UPDATE_PATTERN = raw"(UPDATE|update)\s+($SPECIAL_WORD)\s+(SET|set)\s+($SPECIAL_WORD\s+=\s+$SPECIAL_WORD)(\s*,\s*$SPECIAL_WORD\s+=\s+$SPECIAL_WORD)*(\s+(WHERE|where) .+)?\s*;\s*".r


  val INSERT_PATTERN02 = "^INSERT\\s+(IGNORE\\s+)?INTO\\s+[\\w\\d_]+\\s+\\(([\\w\\d_]+(,\\s)?)+\\)\\s+VALUES\\s+\\((.+)(,\\s.+)*\\)\\s*;?$".r
}
   */

  val INSERT_PATTERN = "^\\s*(?i)INSERT\\s+(IGNORE\\s+)?(?i)INTO\\s+([^\\s(]+)\\s*\\(([^\\)]+)\\)\\s*(?i)VALUES\\s*(?:\\(([^\\)]+)\\)\\s*,\\s*)*\\(([^\\)]+)\\)\\s*$".r
  val SELECT_PATTERN = "^\\s*(?i)SELECT\\s+(.+)\\s+(?i)FROM\\s+([^\\s]+)(?:\\s+(?i)WHERE\\s+(.+))?\\s*$".r
  val UPDATE_PATTERN = "^\\s*(?i)UPDATE\\s+([^\\s]+)\\s+(?i)SET\\s+(.+)\\s+(?i)WHERE\\s+(.+)\\s*$".r


  var taintMemory = new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))

  val debugMode = true

  def doAnalyze(string:String, taintMemorySQL: SqlTaintMemory): Boolean ={
    taintMemory = taintMemorySQL;
    doAnalyze(string)
  }

  def doAnalyze(string: String) = {


    if(string.isBlank) println("yes blank")
    if(debugMode)println("String to analyze: " + string)

    val normalizedString = if(!string.isBlank) preprozesing(string) else ""

    normalizedString match {
      case i@INSERT_PATTERN(_*) =>
        analyzeInsertStatement(i)

      case u@UPDATE_PATTERN(_*) =>
        analyzeUpdateStatement(u)

      case s@SELECT_PATTERN(_*) =>
        analyzeSelectStatment(s)

      case _ =>
        if(debugMode) println("Result of SqlAnalyzer: SqlAnalyzer does not support: "+ string)
        false
    }
  }

  def preprozesing(string: String) = {
    val hasCorrectSyntax = checkSQLStringSyntax3(string)
    val normalizedString = if (hasCorrectSyntax) normalizeSQLString(string) else ""
    if(debugMode && !hasCorrectSyntax ){
      println("Wrong Syntax :" + string)
    }
    normalizedString
  }

  def checkSQLStringSyntax3(input: String): Boolean = {

    input match {
      case INSERT_PATTERN(_*) => true
      case SELECT_PATTERN(_*) => true
      case UPDATE_PATTERN(_*) => true
      case _ => false
    }
  }

  def normalizeSQLString(sqlString: String): String = {

    val sb = new StringBuilder(sqlString)
    val pattern1 = raw"(\s+)"
    val pattern2 = raw"([,;=()])"
    val replacement1 = " "
    val replacement2 = " $1 "

    // Doppelte Leerzeichen entfernen
    sb.replace(0, sb.length(), sb.toString().replaceAll(pattern1, replacement1))

    // Sonderzeichen durch ein Leerzeichen umgeben
    sb.replace(0, sb.length(), sb.toString().replaceAll(pattern2, replacement2))

    // Groß-/Kleinschreibung normalisieren und führenden/folgnde leerzeichen entfernen
    val normalizedStrig =  sb.toString().toUpperCase().trim

    if(debugMode) println("String normalized to: " + normalizedStrig)
    normalizedStrig
  }

  def extractInformationOfInsert4(query: String): (String, Seq[String], Seq[Seq[String]]) = {
    val insertPattern = "(?i)INSERT\\s*(?:IGNORE\\s+)?INTO\\s*(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*(.*);?".r


    var extractedTableName = ""
    var extractedColumnNames: Seq[String] = Seq()
    var extractedValueGroups: Seq[Seq[String]] = Seq()

    query match {
      case insertPattern(tableName, columns, values) =>
        extractedTableName = tableName
        extractedColumnNames = columns.split(",").map(_.trim).toSeq
        val removedBrackets = values.split("\\)\\s*,\\s*\\(").map(x => x.replaceAll(raw"(\(|\))", ""))
        extractedValueGroups = removedBrackets.map(x => x.split(",").map(_.trim).toSeq).toSeq

      case _ =>
    }
    (extractedTableName, extractedColumnNames, extractedValueGroups)
  }


  def analyzeInsertStatement(normalizedString: String) = {
    val (table, columns, valueGroups) = extractInformationOfInsert4(normalizedString)
    var taintedColumns: Set[String] = columns.filter(column => taintMemory.isTainted(column)).toSet

    valueGroups.foreach(valueGroup  => {
      for (i <- valueGroup.indices) {
        if (taintMemory.isTainted(valueGroup (i))) taintedColumns += columns(i).trim
      }
    })

    if (taintMemory.isTainted(table)) taintedColumns ++= columns.map(str => str.trim)
    if (taintedColumns.nonEmpty) taintMemory.taintTableAndColums(table.trim, taintedColumns)
    if(debugMode) println("Result of analyzeInsertStatement:  \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintedColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted \n")

    taintedColumns.nonEmpty

  }

  def extractInformationOfSelect(normalizedString: String): (String, Seq[String]) ={

    val selectPatter01 = raw"(SELECT) (.+) FROM ((\w|-|'|´)+) (WHERE (.+))?; ".r
    var tableNameReturn = ""
    var columnNamesReturn: Seq[String] = Seq()
    normalizedString match {
      case selectPatter01(_, columns, tableName, _, _, condition) =>
        columnNamesReturn = columns.split(",").map(_.trim).toSeq
        tableNameReturn = tableName
      case _ =>
    }
    (tableNameReturn, columnNamesReturn)
  }

  def extractInformationOfSelect2(normalizedString: String): (String, Seq[String]) = {
    val selectRegex = "SELECT\\s+(DISTINCT\\s+)?(.*?)\\s+FROM\\s+([^\\s]+)\\s*(WHERE\\s+(.*))?".r
    var tableNameReturn = ""
    var columnNamesReturn: Seq[String] = Seq()
    selectRegex.findFirstMatchIn(normalizedString) match {
      case Some(str) =>
         columnNamesReturn = str.group(2).split(",").map(_.trim).toSeq
         tableNameReturn = str.group(3)
      case _ =>
    }
    (tableNameReturn, columnNamesReturn)
  }



  def analyzeSelectStatment(normalizedString: String) = {
    val (table, columns) = extractInformationOfSelect2(normalizedString)
    var tempColumn: Set[String] = columns.toSet

    if (columns.contains("*")) {

      taintMemory.taintedTableAndColumns.get(table) match {
        case Some(x) => tempColumn = x
        case None =>
      }
    }

    if (taintMemory.isTainted(table)) taintMemory.taintTableAndColums(table, tempColumn)
    val (hastaints, taintedColumns) = taintMemory.columnsAreTainted(table, tempColumn)
    if(debugMode)println("Result of analyzeSelectStatment: \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintedColumns.mkString("(", ",", ")") + " of table " + table + " where tainted \n")

    hastaints
  }

  def extractInformationOfUpdate(normalizedString: String) = {

    val updatePattern = raw"(UPDATE) (.+) SET (.+) ; ".r
    val valueAndConditionPattern = raw"('?\w+'? = '?\w+'? (, '?\w+'? = '?\w+'? )*)(WHERE .+)?".r

    var table = ""
    var temp03: Set[(String, String)] = Set()

    normalizedString match {
      case updatePattern(_, tableName, valuesAndCondition) =>
        table = tableName

        valuesAndCondition match {
          case valueAndConditionPattern(a, _*) =>
            val temp = a.split(",").map(_.trim).toSet
            temp03 = temp.map(str => str.split("=").map(_.trim)).map(array => (array(0), array(1)))
          case _ =>
        }
      case _ =>
    }

    (table, temp03)
  }

  def extractInformationOfUpdate2(normalizedString: String): (String, Seq[(String, String)]) = {
    val updateRegex = "UPDATE\\s+([^\\s]+)\\s+SET\\s+(.+?)\\s+WHERE\\s+(.+?)\\s*;?\\s*$".r

    var table = ""
    var columnValues: Seq[(String, String)] = Seq()

    updateRegex.findFirstMatchIn(normalizedString) match {
      case Some(m) =>
        table = m.group(1)
        columnValues = m.group(2).split(",").map(_.trim).toIndexedSeq.map { value =>
        val parts = value.split("=").map(_.trim)
          (parts(0), parts(1))
        }
      case _ =>
    }
    (table, columnValues)
  }

  def analyzeUpdateStatement(normalizedString: String) = {
    val (table, columnAndValues) = extractInformationOfUpdate2(normalizedString)
    var taintColumns: Set[String] = Set()

    for ((colmn, value) <- columnAndValues) {
      if (taintMemory.isTainted(value) || taintMemory.isTainted(colmn) || taintMemory.isTainted(table)) {
        taintColumns += colmn
      }
    }

    if (taintColumns.nonEmpty) taintMemory.taintTableAndColums(table, taintColumns)
    if(debugMode)println("Result of analyzeUpdateStatment:  \n"
      + "  analyzed String: " + normalizedString + " \n  "
      + taintColumns.mkString("(", ",", ")") + " of table " + table + " will be tainted")

    taintColumns.nonEmpty

  }

  /*
def extractInformationOfInsert(normalizedString: String) = {

  val insertPatter01 = raw"(INSERT (IGNORE\s)?INTO) (.+) (\( .+ \)) (VALUES) (\(.+\)) ; ".r

  val columnNamesRegex = "\\(([^)]*)\\)".r
  val valuesRegex = "VALUES\\s*\\(([^)]*)\\)".r
  val insertRegex = raw"(INSERT (IGNORE\s)?INTO) (.+) (\( .+ \)) (VALUES) (\(.+\)) ; ".r


  var extractedTableName = ""
  var extractedColumnNames: Seq[String] = Seq()
  var extractedValueGroups: Set[Array[String]] = Set(Array())


  normalizedString match {
    case insertPatter01(_, _, tableName, columnNames, _, values) =>
      extractedTableName = tableName
      extractedColumnNames = columnNames.replaceAll(raw"(\(|\))", "").split(",").map(_.trim).toSeq
      val removedBrackets = values.split(raw"\) , \(").map(x => x.replaceAll(raw"(\(|\))", ""))
      extractedValueGroups = removedBrackets.map(x => x.split(",").map(_.trim)).toSet
    case insertRegex(_, _, tableName, columnNamesRegex(columnNames), _, valuesRegex(values)) =>
      println("klappt")

    case _ =>
  }

  (extractedTableName, extractedColumnNames, extractedValueGroups)

}
 */
}
